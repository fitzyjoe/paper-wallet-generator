package com.ptcoded.paperwallet;

import com.google.zxing.WriterException;
import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.crypto.fips.FipsSHS;
import org.bouncycastle.crypto.general.SecureHash;
import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;

public class Generator
{
    // bounds of private key for bitcoin
    private static final String MAX_PRIVATE_KEY = "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364140";
    private static final String MIN_PRIVATE_KEY = "0000000000000000000000000000000000000000000000000000000000000001";

    // bitcoin specific bytes
    public static final byte PRIVATE_KEY_PREFIX = (byte) 0x80;
    public static final byte[] PRIVATE_KEY_PREFIX_ARRAY = {PRIVATE_KEY_PREFIX};
    public static final byte PRIVATE_KEY_SUFFIX = 0x01;
    public static final byte[] PRIVATE_KEY_SUFFIX_ARRAY = {PRIVATE_KEY_SUFFIX};
    public static final byte[] PUBLIC_KEY_COMPRESSED_Y_EVEN_PREFIX = {0x02};
    public static final byte[] PUBLIC_KEY_COMPRESSED_Y_ODD_PREFIX = {0x03};

    // bouncy castle to provide algorithms
    private static final Provider provider = new BouncyCastleFipsProvider();

    public static void main(String[] args) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, IOException, WriterException
    {
        final var generator = new Generator();
        generator.generatePaperWallet();
    }

    public void generatePaperWallet() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, IOException, WriterException
    {
        // private key
        final var privateKey = generatePrivateKey();
        final var privateKeyWIF = convertPrivateKeyToWIF(privateKey);

        // public key
        final var publicKey = generatePublicKeyCompressed(privateKey);
        final var addressWIF = convertCompressedPublicKeyToAddress(publicKey);

        // make pdf
        final var paperWalletPDFWriter = new PaperWalletPDFWriter();
        paperWalletPDFWriter.generatePDF(privateKeyWIF, addressWIF);
    }

    public static byte[] generatePrivateKey() throws NoSuchAlgorithmException
    {
        SecureRandom sr = SecureRandom.getInstanceStrong();

        byte[] key = new byte[32];

        // generate the private key, but don't let it be out of bounds
        do
        {
            sr.nextBytes(key);
        }
        while (ArrayUtil.bytesToHex(key).compareTo(MAX_PRIVATE_KEY) >= 0 && ArrayUtil.bytesToHex(key).compareTo(MIN_PRIVATE_KEY) <= 0);

        return key;
    }

    /**
     * Converts a raw private key to WIF format.
     * @param privateKey
     * @return
     * @throws NoSuchAlgorithmException
     */
    private static String convertPrivateKeyToWIF(final byte[] privateKey) throws NoSuchAlgorithmException
    {
        final var extendedKey = ArrayUtil.concat(PRIVATE_KEY_PREFIX_ARRAY, privateKey, PRIVATE_KEY_SUFFIX_ARRAY);
        final var sha = MessageDigest.getInstance(FipsSHS.Algorithm.SHA256.getName());
        final var hash = sha.digest(sha.digest(extendedKey));
        final var checksum = Arrays.copyOfRange(hash, 0, 4);
        final var extendedKeyWithChecksum = ArrayUtil.concat(extendedKey, checksum);
        return Base58.encode(extendedKeyWithChecksum);
    }

    /**
     * Given the private key this computes the public key in compressed format (the even/odd prefix and the x coord)
     *
     * @param privateKey the raw private key
     * @return
     * @throws NoSuchAlgorithmException
     * @throws InvalidAlgorithmParameterException
     */
    private static byte[] generatePublicKeyCompressed(final byte[] privateKey) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException
    {
        final var keyGen = KeyPairGenerator.getInstance("EC", provider);
        final var ecSpec = new ECGenParameterSpec("secp256k1");
        keyGen.initialize(ecSpec, new SecureRandom());
        final var ecp = SECNamedCurves.getByName("secp256k1");

        final var privateKeyBigInt = new BigInteger(1, privateKey);

        // multiply the private key by the "generator point"
        final var point = ecp.getG().multiply(privateKeyBigInt);
        final var normed = point.normalize();
        final var x = normed.getXCoord().getEncoded();
        final var y = normed.getYCoord().getEncoded();
        final var publicKeyBigInt = new BigInteger[]{new BigInteger(1, x), new BigInteger(1, y)};
        final var xs = ArrayUtil.bigIntegerToBytes(publicKeyBigInt[0], 32);
        final var ys = ArrayUtil.bigIntegerToBytes(publicKeyBigInt[1], 32);
        byte[] publicKeyWithCompressedBit;
        if ((ys[31] & 0xff) % 2 == 0)
        {
            publicKeyWithCompressedBit = ArrayUtil.concat(PUBLIC_KEY_COMPRESSED_Y_EVEN_PREFIX, xs);
        }
        else
        {
            publicKeyWithCompressedBit = ArrayUtil.concat(PUBLIC_KEY_COMPRESSED_Y_ODD_PREFIX, xs);
        }
        return publicKeyWithCompressedBit;
    }

    /**
     * Converts a public key in compressed format to a bitcoin address in base58.
     * @param publicKeyWithCompressedBit
     * @return
     * @throws NoSuchAlgorithmException
     */
    public static String convertCompressedPublicKeyToAddress(final byte[] publicKeyWithCompressedBit) throws NoSuchAlgorithmException
    {
        final var sha = MessageDigest.getInstance(FipsSHS.Algorithm.SHA256.getName());
        final var s1 = sha.digest(publicKeyWithCompressedBit);
        final var rmd = MessageDigest.getInstance(SecureHash.Algorithm.RIPEMD160.getName(), provider);
        final var r1 = rmd.digest(s1);

        // pad r1 with a zero at the first position
        final var r2 = new byte[r1.length + 1];
        r2[0] = 0;
        System.arraycopy(r1, 0, r2, 1, r1.length);

        final var s2 = sha.digest(sha.digest(r2));
        final var a1 = new byte[25];
        System.arraycopy(r2, 0, a1, 0, 21);
        System.arraycopy(s2, 0, a1, 21, 4);

        return Base58.encode(a1);
    }
}