if test -f "/secret/serviceuser/username"; then
  export SERVICEUSER_USERNAME=$(cat /secret/serviceuser/username)
  echo "Eksporterer variabel SERVICEUSER_USERNAME"
else
  echo "Eksporterer IKKE variabel SERVICEUSER_USERNAME fordi filen /secret/serviceuser/username ikke finnes"
fi

if test -f "/secret/serviceuser/password"; then
  export SERVICEUSER_PASSWORD=$(cat /secret/serviceuser/password)
  echo "Eksporterer variabel SERVICEUSER_PASSWORD"
else
  echo "Eksporterer IKKE variabel SERVICEUSER_PASSWORD fordi filen /secret/serviceuser/password ikke finnes"
fi
