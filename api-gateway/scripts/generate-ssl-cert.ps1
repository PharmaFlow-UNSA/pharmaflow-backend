# Windows PowerShell script to generate SSL certificate

$KEYSTORE_FILE = "..\src\main\resources\keystore.p12"
$KEYSTORE_PASSWORD = "changeit"
$KEY_ALIAS = "pharmaflow-gateway"
$VALIDITY_DAYS = 365

Write-Host "🔐 Generating SSL certificate for API Gateway..." -ForegroundColor Cyan

# Generate keystore with self-signed certificate
keytool -genkeypair `
  -alias $KEY_ALIAS `
  -keyalg RSA `
  -keysize 2048 `
  -storetype PKCS12 `
  -keystore $KEYSTORE_FILE `
  -validity $VALIDITY_DAYS `
  -storepass $KEYSTORE_PASSWORD `
  -dname "CN=localhost, OU=PharmaFlow, O=PharmaFlow, L=Sarajevo, ST=FBiH, C=BA"

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ SSL certificate generated successfully!" -ForegroundColor Green
    Write-Host "📁 Location: $KEYSTORE_FILE" -ForegroundColor Yellow
    Write-Host "🔑 Password: $KEYSTORE_PASSWORD" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "⚠️  For production, replace with a valid certificate from:" -ForegroundColor Red
    Write-Host "   - Let's Encrypt (certbot)"
    Write-Host "   - Your organization's CA"
    Write-Host "   - Commercial CA (DigiCert, GlobalSign, etc.)"
} else {
    Write-Host "❌ Failed to generate certificate" -ForegroundColor Red
}

