# Environment Variables Setup Guide

**Quick Reference for Keycloak Integration**

---

## 📋 Required Environment Variables

### Gateway Service
```
KEYCLOAK_CLIENT_SECRET=<extract-from-keycloak-admin-console>
```

---

## 🔧 How to Extract Client Secrets

### Step 1: Access Keycloak Admin Console
1. Open: http://localhost:8181
2. Login as admin
3. Select realm: **ecommerce**

### Step 2: Extract Gateway Client Secret
1. Click **Clients** in left sidebar
2. Click **gateway-client**
3. Go to **Credentials** tab
4. Copy the **Client Secret** value
5. Save it: `KEYCLOAK_CLIENT_SECRET=<paste-here>`

### Step 3: Extract Order Service Client Secret (Optional)
1. Click **Clients** → **order-service**
2. Go to **Credentials** tab
3. Copy secret (needed for direct testing only)
4. Save it: `ORDER_SERVICE_CLIENT_SECRET=<paste-here>`

### Step 4: Extract Inventory Service Client Secret (Optional)
1. Click **Clients** → **inventory-service**
2. Go to **Credentials** tab
3. Copy secret (needed for direct testing only)
4. Save it: `INVENTORY_SERVICE_CLIENT_SECRET=<paste-here>`

---

## 🖥️ How to Set Environment Variables

### Windows PowerShell (Current Session)
```powershell
# Gateway (REQUIRED)
$env:KEYCLOAK_CLIENT_SECRET="fK8dX2nP9mQ3wR5tY7uV1bN4cZ6hJ8lM"

# Verify
echo $env:KEYCLOAK_CLIENT_SECRET
```

### Windows PowerShell (Permanent)
```powershell
# User-level (persists across sessions)
[System.Environment]::SetEnvironmentVariable("KEYCLOAK_CLIENT_SECRET", "your-secret-here", "User")

# Restart terminal after setting
```

### Windows CMD (Current Session)
```cmd
set KEYCLOAK_CLIENT_SECRET=fK8dX2nP9mQ3wR5tY7uV1bN4cZ6hJ8lM
echo %KEYCLOAK_CLIENT_SECRET%
```

### Linux/macOS Bash (Current Session)
```bash
export KEYCLOAK_CLIENT_SECRET="fK8dX2nP9mQ3wR5tY7uV1bN4cZ6hJ8lM"
echo $KEYCLOAK_CLIENT_SECRET
```

### Linux/macOS (Permanent)
```bash
# Add to ~/.bashrc or ~/.zshrc
echo 'export KEYCLOAK_CLIENT_SECRET="your-secret-here"' >> ~/.bashrc

# Reload
source ~/.bashrc
```

---

## ✅ Verification

### Check Variable is Set (PowerShell)
```powershell
if ($env:KEYCLOAK_CLIENT_SECRET) {
    Write-Host "✅ KEYCLOAK_CLIENT_SECRET is set" -ForegroundColor Green
} else {
    Write-Host "❌ KEYCLOAK_CLIENT_SECRET is NOT set" -ForegroundColor Red
}
```

### Check Variable is Set (Bash)
```bash
if [ -n "$KEYCLOAK_CLIENT_SECRET" ]; then
    echo "✅ KEYCLOAK_CLIENT_SECRET is set"
else
    echo "❌ KEYCLOAK_CLIENT_SECRET is NOT set"
fi
```

---

## 🚀 Start Services with Environment Variables

### Option 1: Set First, Then Run
```powershell
# Set variable
$env:KEYCLOAK_CLIENT_SECRET="your-secret"

# Start Gateway
cd gateway-service
mvn spring-boot:run
```

### Option 2: Inline Environment Variable (PowerShell)
```powershell
# Windows PowerShell 7+
$env:KEYCLOAK_CLIENT_SECRET="your-secret"; mvn spring-boot:run
```

### Option 3: Inline Environment Variable (Bash)
```bash
# Linux/macOS
KEYCLOAK_CLIENT_SECRET="your-secret" mvn spring-boot:run
```

---

## 🔒 Security Best Practices

### ✅ DO
- Store secrets in environment variables
- Use different secrets for dev/staging/production
- Rotate secrets regularly (every 90 days)
- Use secret management tools in production (Azure Key Vault, AWS Secrets Manager)

### ❌ DON'T
- Commit secrets to Git
- Hard-code secrets in application.yml
- Share secrets via email or chat
- Use same secret across environments

---

## 📝 Example Client Secrets (For Reference)

**NOTE:** These are placeholder examples. Use your actual secrets from Keycloak.

```
KEYCLOAK_CLIENT_SECRET=fK8dX2nP9mQ3wR5tY7uV1bN4cZ6hJ8lM
ORDER_SERVICE_CLIENT_SECRET=aB1cD2eF3gH4iJ5kL6mN7oP8qR9sT0u
INVENTORY_SERVICE_CLIENT_SECRET=zY9xW8vU7tS6rQ5pO4nM3lK2jI1hG0f
```

---

## 🐛 Troubleshooting

### Error: "Failed to load ApplicationContext"
**Cause:** Environment variable not set

**Solution:**
```powershell
# Check if variable exists
echo $env:KEYCLOAK_CLIENT_SECRET

# If empty, set it
$env:KEYCLOAK_CLIENT_SECRET="<your-secret>"

# Restart service
mvn spring-boot:run
```

### Error: "invalid_client" from Keycloak
**Cause:** Wrong client secret

**Solution:**
1. Re-check secret in Keycloak Admin Console
2. Copy exact value (no spaces, no quotes)
3. Set environment variable again
4. Restart Gateway

### Gateway Logs Show "${KEYCLOAK_CLIENT_SECRET}"
**Cause:** Variable not resolved (still placeholder)

**Solution:**
```powershell
# Variable was not set before starting service
# Stop service (Ctrl+C)
# Set variable
$env:KEYCLOAK_CLIENT_SECRET="actual-secret-here"
# Restart
mvn spring-boot:run
```

---

## 📚 Related Documentation

- [KEYCLOAK-SETUP-INSTRUCTIONS.md](KEYCLOAK-SETUP-INSTRUCTIONS.md) - Step 8: Extract Client Secrets
- [SECURITY-TESTING-GUIDE.md](SECURITY-TESTING-GUIDE.md) - Prerequisites section
- [SECURITY-IMPLEMENTATION-SUMMARY.md](SECURITY-IMPLEMENTATION-SUMMARY.md) - Full implementation overview

---

## ✅ Quick Checklist

Before starting services:

- [ ] Keycloak is running on port 8181
- [ ] Extracted gateway-client secret from Keycloak
- [ ] Set `KEYCLOAK_CLIENT_SECRET` environment variable
- [ ] Verified variable is set: `echo $env:KEYCLOAK_CLIENT_SECRET`
- [ ] Variable shows actual secret value (not placeholder)
- [ ] Ready to start Gateway service

---

**Setup Time:** ~2 minutes  
**Security Level:** Required for Gateway authentication
