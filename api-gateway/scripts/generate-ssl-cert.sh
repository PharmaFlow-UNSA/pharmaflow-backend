#!/bin/bash

# Generate self-signed SSL certificate for development/testing
# For production, use Let's Encrypt or your organization's CA

KEYSTORE_FILE="../src/main/resources/keystore.p12"
KEYSTORE_PASSWORD="changeit"
KEY_ALIAS="pharmaflow-gateway"
VALIDITY_DAYS=365

echo "🔐 Generating SSL certificate for API Gateway..."

# Generate keystore with self-signed certificate
keytool -genkeypair \
  -alias "$KEY_ALIAS" \
  -keyalg RSA \
  -keysize 2048 \
  -storetype PKCS12 \
  -keystore "$KEYSTORE_FILE" \
  -validity $VALIDITY_DAYS \
  -storepass "$KEYSTORE_PASSWORD" \
  -dname "CN=localhost, OU=PharmaFlow, O=PharmaFlow, L=Sarajevo, ST=FBiH, C=BA"

echo "✅ SSL certificate generated successfully!"
echo "📁 Location: $KEYSTORE_FILE"
echo "🔑 Password: $KEYSTORE_PASSWORD"
echo ""
echo "⚠️  For production, replace with a valid certificate from:"
echo "   - Let's Encrypt (certbot)"
echo "   - Your organization's CA"
echo "   - Commercial CA (DigiCert, GlobalSign, etc.)"

