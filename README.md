# NuitInfo2015Android
Appli Android de la nuit de l'info 2015

L'application permet aux personnes en danger de se manifester au central, qui donnera la position de cette personne sur un site web. Elle permet également de prevenir les utilisateurs présent dans une "zone a risque" par une notification.

## Partie Android

Application Android classique, un Boutton de type Toggle, permet de passer en mode Alerte/Securisé. En cas d'alerte, elle envoie les coordonées gps au serveur. Tant qu'elle tourne, l'application verifie via une requete si il n'y a pas d'utilisateur en danger à proximité.

Des screens de l'application sont disponible dans le repertoire img du dépôt Git.

lien GitHub : https://github.com/Mandrivia/NuitInfo2015Android

## Partie Serveur

Le serveur traite les requêtes et permet d'afficher une mapmonde en 3D (WebGL) en temps réel des utilisateurs en danger.

lien GitHub : https://github.com/Mandrivia/NuitInfo2015


