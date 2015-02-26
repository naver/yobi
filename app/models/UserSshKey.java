/**
 * Yobi, Project Hosting SW
 *
 * Copyright 2015 NAVER Corp.
 * http://yobi.io
 *
 * @author KiSeong Park
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
package models;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Date;

import javax.persistence.*;

import models.User;

import org.apache.commons.codec.binary.Base64;
import org.apache.sshd.common.SshException;
import org.apache.sshd.common.util.Buffer;
import org.eclipse.jgit.lib.Constants;

import play.data.format.Formats;
import play.data.validation.Constraints;
import play.db.ebean.*;

@Entity
public class UserSshKey extends Model {
    private static final long serialVersionUID = -1971453347598433331L;


    @Transient
    private String rawData;
    @Transient
    private PublicKey key;


    /*
     * DSA Public key length, encoded to BASE64. (588)
     * DSA Public Key's length is fixed (only 1024bit)
     * if RSA Public Key's length over (588), UserApp.ValidationSshKey rejected. (RSA 2048 < DSA 1024)
     */
    @Id
    @Column(columnDefinition="varchar(588)")
    @Constraints.Required
    public String publicKey;

    @ManyToOne
    public User user;
    public String comment;

    public String fingerPrint;

    @Formats.DateTime(pattern = "yyyy-MM-dd")
    public Date lastUsedDate;

    @Formats.DateTime(pattern = "yyyy-MM-dd")
    public Date registerDate;

    public static final Finder<String, UserSshKey> find = new Finder<String, UserSshKey>(String.class, UserSshKey.class);

    public UserSshKey (final String data, String comment) {
        this.rawData = data;
        this.comment = comment;
        makePublicKeyB64();
        makeComment();
        makeFingerprint();
    }

    public UserSshKey (final PublicKey key) {
        this.key = key;
        this.comment = "";
    }

    public boolean isEmpty(final String value) {
        return value == null || value.trim().length() == 0;
    }

    public String makeComment() {
        if (isEmpty(comment) && rawData != null) {
            final String[] parts = rawData.split(" ", 3);
            if (parts.length == 3) {
                comment = parts[2];
            } else {
                comment = parts[1].substring(0,18);
            }
        }
        return comment;
    }

    public String makeFingerprint() {
        if (fingerPrint == null) {
            final StringBuilder sBuilder = new StringBuilder();
            String hash;
            if (rawData == null) {
                hash = getMD5(getKey().getEncoded());
            } else {
                final String[] parts = rawData.split(" ", 3);
                final byte [] bin = Base64.decodeBase64(Constants.encodeASCII(parts[1]));
                hash = getMD5(bin);
            }
            for (int i = 0; i < hash.length(); i += 2) {
                sBuilder.append(hash.charAt(i)).append(hash.charAt(i + 1)).append(':');
            }
            sBuilder.setLength(sBuilder.length() - 1);
            fingerPrint = sBuilder.toString();
        }
        return fingerPrint;
    }

    public String makePublicKeyB64() {
        if(publicKey == null){
            if (rawData == null && key != null) {
                return makePublicKeyB64(key);
            } else if (rawData != null) {
                String[] parts = rawData.split(" ", 3);
                if (parts.length > 1) {
                    publicKey = parts[0] +" "+ parts[1];
                } else {
                    return null;
                }
            }
        }
        return publicKey;
    }

    public static String makePublicKeyB64(PublicKey key){
        final Buffer buf = new Buffer();

        buf.putRawPublicKey(key);
        final String alg = buf.getString();

        buf.clear();
        buf.putPublicKey(key);
        final String b64 = Base64.encodeBase64String(buf.getBytes());

        return alg + " " + b64;
    }

    public PublicKey getKey() {
        if (key == null && rawData != null) {
            final String[] parts = rawData.split(" ", 3);
            if (comment == null && parts.length == 3) {
                comment = parts[2];
            }
            final byte[] bin = Base64.decodeBase64(Constants.encodeASCII(parts[1]));
            try {
                key = new Buffer(bin).getRawPublicKey();
            } catch (SshException e) {
                throw new RuntimeException(e);
            }
        }
        return key;
    }

    private String getMD5(final byte... bytes) {
        try {
            final MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.reset();
            md5.update(bytes);
            final byte[] digest = md5.digest();

            final StringBuilder sBuilder = new StringBuilder(digest.length * 2);
            for (int i=0; i<digest.length; i++) {
                if ((digest[i] & 0xff) < 0x10) {
                    sBuilder.append('0');
                }
                sBuilder.append(Long.toString(digest[i] & 0xff, 16));
            }

            return sBuilder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public String getAlgorithm() {
        return getKey().getAlgorithm();
    }

    public boolean isUsed(){
        if(lastUsedDate==null) return false;
        return true;
    }

    public void updateLastUsedDate() {
        this.lastUsedDate = new Date();
        this.save();
    }

    public static String register(UserSshKey key) {
        key.registerDate = new Date();
        key.save();
        return key.publicKey;
    }

    public static UserSshKey findByKey(String keyString) {
        UserSshKey findKey = find.where().eq("publicKey", keyString).findUnique();
        return findKey;
    }
}
