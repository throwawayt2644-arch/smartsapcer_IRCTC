package com.meilluer.smartspacer_irctc.service

import com.meilluer.smartspacer_irctc.data.TicketRepository
import jakarta.mail.Flags
import jakarta.mail.Folder
import jakarta.mail.Session
import jakarta.mail.Store
import jakarta.mail.internet.MimeMessage
import jakarta.mail.search.AndTerm
import jakarta.mail.search.FlagTerm
import jakarta.mail.search.FromStringTerm
import jakarta.mail.search.SubjectTerm
import java.util.Properties

class EmailScanner {

    fun scanEmails(email: String, appPassword: String, onlyUnread: Boolean = true, customSender: String? = null, customSubject: String? = null): Boolean {
        val props = Properties()
        props["mail.store.protocol"] = "imaps"
        props["mail.imaps.host"] = "imap.gmail.com"
        props["mail.imaps.port"] = "993"
        props["mail.imaps.ssl.enable"] = "true"
        props["mail.imaps.peek"] = "true" // Prevent auto-marking as read

        var store: Store? = null
        var inbox: Folder? = null

        try {
            val session = Session.getDefaultInstance(props)
            store = session.getStore("imaps")
            store.connect("imap.gmail.com", email, appPassword)

            inbox = store.getFolder("INBOX")
            // Open in READ_WRITE so we can mark as read
            inbox.open(Folder.READ_WRITE)

            // Search for IRCTC confirmation emails
            val senderEmail = customSender ?: "ticketadmin@irctc.co.in"
            val senderTerm = FromStringTerm(senderEmail)
            
            val subjectStr = customSubject ?: "Booking Confirmation on IRCTC"
            val subjectTerm = SubjectTerm(subjectStr)
            
            val baseTerm = if (customSender != null && customSubject == null) {
                // If custom sender is provided but no subject, we still prefer the default subject
                // but we could also just search for the sender. 
                // Let's stick to subject + sender for safety, but allow it to be broader.
                AndTerm(senderTerm, subjectTerm)
            } else {
                AndTerm(senderTerm, subjectTerm)
            }

            val searchTerm = if (onlyUnread) {
                val unreadTerm = FlagTerm(Flags(Flags.Flag.SEEN), false)
                AndTerm(arrayOf(baseTerm, unreadTerm))
            } else {
                baseTerm
            }

            val messages = inbox.search(searchTerm)
            
            // Sort by date descending (latest first)
            messages.sortByDescending { it.sentDate }

            for (message in messages) {
                val content = getTextFromMessage(message)
                
                val parser = IrctcParser()
                val ticketInfo = parser.parseEmail(content)
                
                if (ticketInfo != null) {
                    TicketRepository.currentTicket = ticketInfo
                    
                    // Mark as read ONLY if information is extracted
                    message.setFlag(Flags.Flag.SEEN, true)
                    
                    return true
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            inbox?.close(false)
            store?.close()
        }
        return false
    }

    private fun getTextFromMessage(message: jakarta.mail.Message): String {
        if (message.isMimeType("text/plain")) {
            return message.content.toString()
        } else if (message.isMimeType("multipart/*")) {
            val mimeMultipart = message.content as jakarta.mail.Multipart
            return getTextFromMultipart(mimeMultipart)
        }
        return ""
    }

    private fun getTextFromMultipart(mimeMultipart: jakarta.mail.Multipart): String {
        var result = ""
        val count = mimeMultipart.count
        for (i in 0 until count) {
            val bodyPart = mimeMultipart.getBodyPart(i)
            if (bodyPart.isMimeType("text/plain")) {
                result += bodyPart.content
            } else if (bodyPart.isMimeType("text/html")) {
                val html = bodyPart.content as String
                return html // Prefer HTML for IRCTC emails
            } else if (bodyPart.content is jakarta.mail.Multipart) {
                result += getTextFromMultipart(bodyPart.content as jakarta.mail.Multipart)
            }
        }
        return result
    }
}
