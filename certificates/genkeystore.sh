#!/bin/bash

# 20170218
# Bash script for generating a single client certificate (wrapped in a keystore)
# for project 2 in EIT060. 
# Parameters: name CN O OU

name=$1
cn=$2
o=$3
ou=$4


# Passwords.
capass="password"       # protects certificate authority private key
ckspass="password"      # protects client keystore
ckeypass="password"     # protects client private key

# Distinguished names.
groupcn="${cn}"
dnamec="CN=\"${groupcn}\",OU=${ou},O=${o},L=Lund,S=Scania,C=SE"

cd keystores

create_stores_and_certificate()  {
    alias=$1
    dname=$2
    kspass=$3
    keypass=$4
    kstore=$5

    # Delete keystore, if it exists.
    rm $kstore 2> /dev/null

    # Create our keypair (and keystore).
    keytool -genkeypair -alias $alias -keyalg rsa -keysize 4096 -keystore $kstore -storepass $kspass -keypass $keypass -dname "$dname"

    # Create our certificate request.
    keytool -certreq -alias $alias -file csr -keystore $kstore -storepass $kspass

    # Sign our certificate.
    openssl x509 -req -CA ../cacert.pem -CAkey ../cakey.pem -CAcreateserial -CAserial ../cacert.srl -in csr -out cert.pem -passin pass:${capass}

    # Import CA certificate and our certificate into keystore.
    keytool -importcert -alias ca -file ../cacert.pem -keystore $kstore -storepass $kspass -noprompt
    keytool -importcert -alias $alias -file cert.pem -keystore $kstore -storepass $kspass -noprompt
    
    # Clean up.
    rm csr
    rm cert.pem
}

create_stores_and_certificate "$name" "$dnamec" $ckspass $ckeypass "$name"

