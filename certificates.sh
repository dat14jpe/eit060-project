#!/bin/bash

# 20170201
# Bash script for generating CA certificate and client/server certificates
# for project 1 in EIT060. Uses no parameters, though several variables
# (and a couple of other strings) in here are essentially freely configurable.


# Passwords.
capass="password"       # protects certificate authority private key
ctspass="password"      # protects client truststore
ckspass="password"      # protects client keystore
ckeypass="password"     # protects client private key
stspass="password"      # protects server truststore
skspass="password"      # protects server keystore
skeypass="password"     # protects server private key

# Distinguished names.
groupcn="John Helbrink (mat14jhe), Simon Johansson (tpi13sjo), Johan Pettersson (dat14jpe), Otto Sörnäs (dic15oso)"
dnameca="/C=SE/ST=Scania/L=Lund/O=LTH/OU=Student/CN=CA"
dnamec="CN=\"${groupcn}\",OU=Student,O=LTH,L=Lund,S=Scania,C=SE"

# Create CA certificate and key, stored in separate files.
openssl req -x509 -newkey rsa:4096 -keyout cakey.pem -out cacert.pem -passout pass:${capass} -days 365 -subj $dnameca

create_stores_and_certificate()  {
    alias=$1
    tspass=$2
    kspass=$3
    keypass=$4
    tstore=$5
    kstore=$6

    # Delete truststore/keystore, if they exist.
    rm $tstore 2> /dev/null
    rm $kstore 2> /dev/null

    # Create truststore (by importing CA certificate).
    keytool -importcert -alias ca -file cacert.pem -keystore $tstore -storepass $tspass -noprompt

    # Create our keypair (and keystore).
    keytool -genkeypair -alias $alias -keyalg rsa -keysize 4096 -keystore $kstore -storepass $kspass -keypass $keypass -dname "$dnamec"

    # Create our certificate request.
    keytool -certreq -alias $alias -file clientcsr -keystore $kstore -storepass $kspass

    # Sign our certificate.
    openssl x509 -req -CA cacert.pem -CAkey cakey.pem -CAcreateserial -in clientcsr -out clientcert.pem -passin pass:${capass}

    # Import CA certificate and our certificate into keystore.
    keytool -importcert -alias ca -file cacert.pem -keystore $kstore -storepass $kspass -noprompt
    keytool -importcert -alias $alias -file clientcert.pem -keystore $kstore -storepass $kspass -noprompt
}

create_stores_and_certificate client $ctspass $ckspass $ckeypass clienttruststore clientkeystore
create_stores_and_certificate server $stspass $skspass $skeypass servertruststore serverkeystore

# List client certificates.
#keytool -list -v -keystore clientkeystore -storepass $ckspass

