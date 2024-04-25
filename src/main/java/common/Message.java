package common;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.security.SignedObject;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

/**
 * A serializable message used for user authentication
 *
 * @author Eduardo Proença (57551)
 * @author Manuel Barral (52026)
 * @author Tiago Oliveira (54979)
 */
public class Message implements Serializable {

    private final SignedObject object;
    private Certificate certificate;

    public Message(SignedObject object, Certificate certificate) {
        this.object = object;
        this.certificate = certificate;
    }

    @Serial
    private void writeObject(ObjectOutputStream out)  {
        try {
            out.defaultWriteObject();
            out.writeObject(certificate.getEncoded());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Serial
    private void readObject(ObjectInputStream in) {
        try {
            in.defaultReadObject();
            byte[] b = (byte[]) in.readObject();
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            this.certificate = cf.generateCertificate(new ByteArrayInputStream(b));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public Certificate getCertificate() {
        return certificate;
    }

    public SignedObject getSignedObject() {
        return this.object;
    }
}
