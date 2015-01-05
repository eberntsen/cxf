/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.rs.security.jose.jwe;

import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;

import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.common.util.crypto.CryptoUtils;
import org.apache.cxf.rs.security.jose.JoseConstants;
import org.apache.cxf.rs.security.jose.jwa.Algorithm;
import org.apache.cxf.rs.security.jose.jwk.JwkUtils;


public class EcdhDirectKeyJweEncryption extends DirectKeyJweEncryption {
    public EcdhDirectKeyJweEncryption(ECPublicKey peerPublicKey,
                                      String curve,
                                      String apuString,
                                      String apvString,
                                      String ctAlgo) {
        super(new JweHeaders(ctAlgo), 
              new EcdhAesGcmContentEncryptionAlgorithm(peerPublicKey,
                                                       curve,
                                                       toBytes(apuString),
                                                       toBytes(apvString),
                                                       ctAlgo), 
              new EcdhDirectKeyEncryptionAlgorithm());
    }
    private static byte[] toBytes(String str) {
        return str == null ? null : StringUtils.toBytesUTF8(str);
    }
    protected static class EcdhDirectKeyEncryptionAlgorithm extends DirectKeyEncryptionAlgorithm {
        protected void checkKeyEncryptionAlgorithm(JweHeaders headers) {
            headers.setKeyEncryptionAlgorithm(JoseConstants.ECDH_ES_DIRECT_ALGO);
        }
    }
    protected static class EcdhAesGcmContentEncryptionAlgorithm extends AesGcmContentEncryptionAlgorithm {
        private ECPublicKey peerPublicKey;
        private String ecurve;
        private byte[] apuBytes;
        private byte[] apvBytes;
        public EcdhAesGcmContentEncryptionAlgorithm(ECPublicKey peerPublicKey,
                                                    String curve,
                                                    byte[] apuBytes,
                                                    byte[] apvBytes,
                                                    String ctAlgo) {
            super(ctAlgo);
            this.peerPublicKey = peerPublicKey;
            this.ecurve = curve;
            this.apuBytes = apuBytes;
            this.apvBytes = apvBytes;
        }
        public byte[] getContentEncryptionKey(JweHeaders headers) {
            KeyPair pair = CryptoUtils.generateECKeyPair(ecurve);
            ECPublicKey publicKey = (ECPublicKey)pair.getPublic();
            ECPrivateKey privateKey = (ECPrivateKey)pair.getPrivate();
            return doGetContentEncryptionKey(headers, publicKey, privateKey);
        }
        protected byte[] doGetContentEncryptionKey(JweHeaders headers,
                                                ECPublicKey publicKey,
                                                ECPrivateKey privateKey) {
            Algorithm jwtAlgo = Algorithm.valueOf(super.getAlgorithm());
        
            headers.setHeader("apu", Base64UrlUtility.encode(apuBytes));
            headers.setHeader("apv", Base64UrlUtility.encode(apvBytes));
            headers.setJsonWebKey("epv", JwkUtils.fromECPublicKey(publicKey, ecurve));
            
            return JweUtils.getECDHKey(privateKey, peerPublicKey, apuBytes, apvBytes, 
                                       jwtAlgo.getJwtName(), jwtAlgo.getKeySizeBits());
            
        }
    }
    
}
