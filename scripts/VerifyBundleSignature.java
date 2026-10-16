import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Locale;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/** Require every AAB payload entry to be signed by the preserved upload identity. */
public final class VerifyBundleSignature {
    public static void main(String[] args) throws Exception {
        if (args.length != 3) throw new IllegalArgumentException("bundle.aab upload.p12 alias");
        String password = System.getenv("FALLEY_UPLOAD_STORE_PASSWORD");
        if (password == null || password.isEmpty()) throw new IllegalStateException("Signing password environment is missing");
        KeyStore store = KeyStore.getInstance("PKCS12");
        char[] secret = password.toCharArray();
        try (InputStream input = Files.newInputStream(Path.of(args[1]))) { store.load(input, secret); }
        finally { Arrays.fill(secret, '\0'); }
        Certificate expected = store.getCertificate(args[2]);
        if (expected == null) throw new SecurityException("Upload alias is missing from the preserved key store");
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] identity = sha256.digest(expected.getEncoded());
        int payloads = 0;
        byte[] buffer = new byte[8192];
        try (JarFile bundle = new JarFile(args[0], true)) {
            Enumeration<JarEntry> entries = bundle.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory() || signatureMetadata(entry.getName())) continue;
                // Reading to EOF makes JarFile validate the signed entry digests.
                try (InputStream input = bundle.getInputStream(entry)) { while (input.read(buffer) != -1) { } }
                Certificate[] certificates = entry.getCertificates();
                boolean signedByUploadKey = false;
                if (certificates != null) for (Certificate certificate : certificates)
                    if (MessageDigest.isEqual(identity, sha256.digest(certificate.getEncoded()))) signedByUploadKey = true;
                if (!signedByUploadKey) throw new SecurityException("Unsigned or unexpected signer: " + entry.getName());
                payloads++;
            }
        }
        if (payloads == 0) throw new SecurityException("No signed bundle payload was found");
        StringBuilder fingerprint = new StringBuilder();
        for (byte value : identity) fingerprint.append(String.format(Locale.ROOT, "%02x", value & 255));
        System.out.println("PASS: " + payloads + " payload entries verified against upload certificate SHA-256 " + fingerprint);
    }
    private static boolean signatureMetadata(String name) {
        String upper = name.toUpperCase(Locale.ROOT);
        if (upper.equals("META-INF/MANIFEST.MF")) return true;
        if (!upper.startsWith("META-INF/")) return false;
        String leaf = upper.substring(9);
        return !leaf.contains("/") && (leaf.endsWith(".SF") || leaf.endsWith(".RSA") || leaf.endsWith(".DSA") || leaf.endsWith(".EC"));
    }
}
