package common;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.SignedObject;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

public class Message implements Serializable {
    private SignedObject object;
    private  Certificate certificate;
    public Message(SignedObject object, Certificate certificate) {
        this.object = object;
        this.certificate = certificate;
    }
    public Message(SignedObject object) {
        this.object = object;
    }
    private void writeObject(ObjectOutputStream out)  {
        try {
            out.defaultWriteObject();
            out.writeObject(certificate.getEncoded());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void readObject(ObjectInputStream in) {
        try {
            in.defaultReadObject();
            byte b[] = (byte[]) in.readObject();
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            this.certificate = cf.generateCertificate(new ByteArrayInputStream(b));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Certificate getCertificate() {
        return certificate;
    }


    public SignedObject getSignedObject() {
        return this.object;
    }
}
