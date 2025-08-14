<p align="center">
  <img
    src="https://readme-typing-svg.herokuapp.com?font=Fira+Code&size=28&pause=500&color=1E90FF&center=true&width=600&lines=🔐+myCloud+-+Secure+File+Storage+System"
    alt="Typing animation"
  />
</p>

---

## 🚀 About the Project

**myCloud** is a simplified distributed **secure file storage system** developed in **Java** using the **Java Security API**.  
Its goal is to provide **encryption, digital signature, user authentication, data integrity**, and **secure communication** via **TLS/SSL**.

Main features:
- 📂 **Centralized storage** accessible by multiple clients
- 🔑 **Key management** with individual **keystores**
- 🔐 **Hybrid encryption** for file protection
- ✍️ **Digital signature** and **verification**
- 🔏 **Integrity protection** of the password file via **HMAC**
- 🌐 **Secure communication** between client and server using **TLS/SSL**

---

## 🛠️ Tech Stack

### 💻 Language & Frameworks
<p align="center">
  <img alt="Java" src="https://img.shields.io/badge/Java-007396?logo=openjdk&logoColor=white&style=for-the-badge" />
</p>

### 🔒 Security & Cryptography
<p align="center">
  <img alt="TLS/SSL" src="https://img.shields.io/badge/TLS%2FSSL-003366?logo=letsencrypt&logoColor=white&style=for-the-badge" />
  <img alt="HMAC" src="https://img.shields.io/badge/HMAC-SHA256-orange?style=for-the-badge" />
  <img alt="Keytool" src="https://img.shields.io/badge/Keytool-Java%20Security-green?style=for-the-badge" />
</p>

---

## ⚙️ Features

### 🔐 Encrypt and Decrypt Files
```
myCloud -u username -p password -c file -t recipient
myCloud -u username -p password -d file
```

### ✍️ Sign and Verify File Signature
```
myCloud -u username -p password -a file
myCloud -u username -p password -v file
```

### 📤 Send & 📥 Receive Files
```
myCloud -s IP:port -u username -p password -e files
myCloud -s IP:port -u username -p password -r files
```

### 👤 User Management (server only)
```
criarUser username password
```

---

## 📚 Key Learnings
- Implementation of **hybrid encryption** in Java
- Management of **keystores** and certificates
- **Integrity protection** with **HMAC**
- Secure user authentication with **salt + hash**
- **TLS/SSL** configuration to authenticate the server and encrypt communication

---

## 📸 System Architecture
> Client ↔ **TLS/SSL** ↔ myCloud Server  
> Individual keystores for file encryption/signing  
> Authentication using `users` file protected by MAC

