/**
 * Yobi, Project Hosting SW
 *
 * Copyright 2014 NAVER Corp.
 * http://yobi.io
 *
 * @Author Yi EungJun
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package mailbox;

import com.sun.mail.imap.IMAPMessage;

import javax.activation.DataHandler;
import javax.mail.*;
import javax.mail.search.SearchTerm;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Enumeration;

public class FakeIMAPMessage extends IMAPMessage {
    protected FakeIMAPMessage() {
        super(null);
    }

    @Override
    public Address[] getFrom() throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setFrom(Address address) throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addFrom(Address[] addresses) throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Address getSender() throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSender(Address address) throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Address[] getRecipients(Message.RecipientType type) throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setRecipients(Message.RecipientType type, Address[] addresses) throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addRecipients(Message.RecipientType type, Address[] addresses) throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Address[] getReplyTo() throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setReplyTo(Address[] addresses) throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getSubject() throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSubject(String subject, String charset) throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Date getSentDate() throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSentDate(Date d) throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Date getReceivedDate() throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getSize() throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getLineCount() throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] getContentLanguage() throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setContentLanguage(String[] languages) throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getInReplyTo() throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized String getContentType() throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getDisposition() throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDisposition(String disposition) throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getEncoding() throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getContentID() throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setContentID(String cid) throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getContentMD5() throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setContentMD5(String md5) throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getDescription() throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDescription(String description, String charset) throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getMessageID() throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getFileName() throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setFileName(String filename) throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized DataHandler getDataHandler() throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDataHandler(DataHandler content) throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public InputStream getMimeStream() throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeTo(OutputStream os) throws IOException, MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] getHeader(String name) throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getHeader(String name, String delimiter) throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setHeader(String name, String value) throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addHeader(String name, String value) throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeHeader(String name) throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Enumeration getAllHeaders() throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Enumeration getMatchingHeaders(String[] names) throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Enumeration getNonMatchingHeaders(String[] names) throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addHeaderLine(String line) throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Enumeration getAllHeaderLines() throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Enumeration getMatchingHeaderLines(String[] names) throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Enumeration getNonMatchingHeaderLines(String[] names) throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized Flags getFlags() throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized boolean isSet(Flags.Flag flag) throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized void setFlags(Flags flag, boolean set) throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized void setPeek(boolean peek) {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized boolean getPeek() {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized void invalidateHeaders() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setFrom() throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Address[] getAllRecipients() throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setRecipients(Message.RecipientType type, String addresses) throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addRecipients(Message.RecipientType type, String addresses) throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSubject(String subject) throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isMimeType(String mimeType) throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDescription(String description) throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public InputStream getInputStream() throws IOException, MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public InputStream getRawInputStream() throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getContent() throws IOException, MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setContent(Object o, String type) throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setText(String text) throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setText(String text, String charset) throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setText(String text, String charset, String subtype) throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setContent(Multipart mp) throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Message reply(boolean replyToAll) throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeTo(OutputStream os, String[] ignoreList) throws IOException, MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void saveChanges() throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setRecipient(Message.RecipientType type, Address address) throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addRecipient(Message.RecipientType type, Address address) throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setFlag(Flags.Flag flag, boolean set) throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getMessageNumber() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Folder getFolder() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isExpunged() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean match(SearchTerm term) throws MessagingException {
        throw new UnsupportedOperationException();
    }
}
