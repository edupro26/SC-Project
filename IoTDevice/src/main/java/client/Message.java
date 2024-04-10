package client;

import java.io.Serializable;
import java.security.SignedObject;
import java.security.cert.Certificate;

public class Message implements Serializable {
    private String userId;
    private  long originalNonce;
    private SignedObject object;
    private  Certificate certificate;
    private boolean isRegistered;
    public Message(SignedObject object, Certificate certificate, Long originalNonce) {
        this.object = object;
        this.certificate = certificate;
        this.originalNonce = originalNonce;
    }
    public Message(SignedObject object) {
        this.object = object;
    }


    public Message(long nonce, boolean b) {
        this.originalNonce = nonce;
        this.isRegistered = b;
    }

    public Message(String userId) {
        this.userId = userId;
    }

    //TODO
    private void writeObject()  {

    }

    public Long getOriginalNonce() {
        return originalNonce;
    }

    public boolean isRegistered() {
        return isRegistered;
    }

    public void setRegistered(boolean registered) {
        isRegistered = registered;
    }

    public Certificate getCertificate() {
        return certificate;
    }

    public void setCertificate(Certificate certificate) {
        this.certificate = certificate;
    }

    public long getNonce() {return originalNonce;}

    public void setNonce(long nonce) {
        this.originalNonce = nonce;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public boolean getFlag() {
        return this.isRegistered;
    }

    public SignedObject getSignedObject() {
        return this.object;
    }
}