
<!-- Animated typing header -->
<p align="center">
  <img
    src="https://readme-typing-svg.herokuapp.com?font=Fira+Code&size=28&pause=500&color=1E90FF&center=true&width=600&lines=🔐+myCloud+-+Secure+File+Storage+System"
    alt="Typing animation"
  />
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Status-Active-brightgreen?style=for-the-badge" />
  <img src="https://img.shields.io/badge/License-MIT-blue?style=for-the-badge" />
  <img src="https://img.shields.io/badge/Version-1.0-orange?style=for-the-badge" />
</p>

---

## 🚀 Sobre o Projeto
O **myCloud** é um sistema distribuído simplificado de **armazenamento seguro de ficheiros**, desenvolvido em **Java** usando a **API de segurança do Java**.  
O objetivo é fornecer **cifragem, assinatura digital, autenticação de utilizadores, integridade de dados** e **comunicação segura** via **TLS/SSL**.

Principais características:
- 📂 **Armazenamento centralizado** com acesso por múltiplos clientes
- 🔑 **Gestão de chaves** com **keystores** individuais
- 🔐 **Cifra híbrida** para proteção de ficheiros
- ✍️ **Assinatura digital** e **verificação**
- 🔏 **Proteção de integridade** do ficheiro de passwords via **HMAC**
- 🌐 **Comunicação segura** entre cliente e servidor usando **TLS/SSL**

---

## 🛠️ Tech Stack

### 💻 Linguagem & Frameworks
<p align="center">
  <img alt="Java" src="https://img.shields.io/badge/Java-007396?logo=openjdk&logoColor=white&style=for-the-badge" />
</p>

### 🔒 Segurança & Criptografia
<p align="center">
  <img alt="TLS/SSL" src="https://img.shields.io/badge/TLS%2FSSL-003366?logo=letsencrypt&logoColor=white&style=for-the-badge" />
  <img alt="HMAC" src="https://img.shields.io/badge/HMAC-SHA256-orange?style=for-the-badge" />
  <img alt="Keytool" src="https://img.shields.io/badge/Keytool-Java%20Security-green?style=for-the-badge" />
</p>

---

## ⚙️ Funcionalidades

### 🔐 Cifrar e Decifrar Ficheiros
```
myCloud -u username -p password -c ficheiro -t destinatario
myCloud -u username -p password -d ficheiro
```

### ✍️ Assinar e Validar Assinatura
```
myCloud -u username -p password -a ficheiro
myCloud -u username -p password -v ficheiro
```

### 📤 Enviar & 📥 Receber Ficheiros
```
myCloud -s IP:porto -u username -p password -e ficheiros
myCloud -s IP:porto -u username -p password -r ficheiros
```

### 👤 Gestão de Utilizadores (apenas no servidor)
```
criarUser username password
```

---

## 📚 Lições Aprendidas
- Implementação de **cifra híbrida** em Java
- Gestão de **keystores** e certificados
- Proteção de **integridade** com **HMAC**
- Autenticação segura de utilizadores com **salt + hash**
- Configuração de **TLS/SSL** para autenticar o servidor e cifrar a comunicação

---

## 📸 Arquitetura do Sistema
> Cliente ↔ **TLS/SSL** ↔ Servidor myCloud  
> Keystores individuais para cifrar/assinar ficheiros  
> Autenticação com ficheiro `users` protegido por MAC

---

## 📜 Licença
Este projeto é licenciado sob a **MIT License**.
