Cette application est une application de voyage, je dois donc gerer le cas ou l'utilisateur n'a pas de connexion internet et rendre une partie de cette application utilisable hors ligne.

Ce que je veux c'est que si l'application detecte qu'il n'y a pas de reseau, seul l'onglet trips doit etre accessible.
Je dois pouvoir voir la liste de mes voyages hors lignes mais je ne dois pouvoir rentrer dans le planner et les depenses que des voyages en cours.
Je dois pouvoir, toujours en mode hors ligne, pouvoir ajouter une depense, et un evenement dans le planner (via le formulaire ou la fonction ici et maintenant) uniquement pour un voyage en cours.
Lorsque la connexion revient tout doit etre synchronisé, et sauvegarder sur le serveur et la base de données.
En cas de conflit priorité à la valeur du serveur.
Quand je suis hors ligne , je voudrais un petit bandeau flottant en bas de l'ecran indiquant en attente de synchronisation avec le nombre d'element à synchroniser.
Lors du retour en ligne je veux voir ce bandeau passé à en cours de synchronisation et le nombre d'element synchronisé et ensuite ce bandeau devenir vert avec ecrit synchroinisation reussit et disparaitre au bout de quelques secondes.
Pense bien à mettre à jour les tests unitaires, et à completer le readme et le changelog.