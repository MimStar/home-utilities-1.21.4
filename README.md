# What is this mod?
**HOME Utilities** is a **server-side mod** that adds **new commands** to your server!
This mod only adds **HOME (waypoint) commands**.

# What new commands?
Here is the list :

- **sethome [home_name]** : Save your position to a waypoint name, rewrite the waypoint if it already exists.
- **delhome [home_name]** : Delete the waypoint of your list.
- **home [home_name]** : Teleports you to the position of your waypoint.
- **homes** : Gives you the list of your homes.
- **sharehome [home_name] [player_name]** : Share a home with another player. The target player needs to **click on the message** to add it to his list.
- **psethome [home_name]** : Save your position to a public waypoint name accessible by anyone, rewrite the waypoint if it already exists.
- **pdelhome [home_name]** : Delete the public waypoint only if you're the owner.
- **phome [home_name]** : Teleports you to the position of the public waypoint.
- **phomes** : Gives you the list of the public homes.
- **/homelanguage [language]** : Change the language used by the mod for the player.

# What is the translations system?
With versions 1.2.0+ a new config file _"home_translations.json"_ is automatically generated on server start, you can add translations as below that your players can use with the **/tpalanguage** command (here i added a french translation):

```json
{
  "en": {
    "sethome_success": "Your home has been set!",
    "psethome_success": "Your public home has been set!",
    "delhome_success": "Your home has been deleted!",
    "delhome_failure": "Error : The home don't exist.",
    "pdelhome_success": "Your public home has been deleted!",
    "pdelhome_failure": "Error : The public home don't exist or you're not the owner.",
    "home_success": "You have been teleported to your home!",
    "home_failure": "Error : The home don't exist.",
    "phome_success": "You have been teleported to the public home!",
    "phome_failure": "Error : The public home don't exist.",
    "homes_success": "Your homes (You can click on them to teleport):",
    "homes_failure": "Error : You don't have any home.",
    "phomes_success": "Public homes (You can click on them to teleport):",
    "phomes_failure": "Error : The server don't have any public home.",
    "sharehome_success": "%s wants to share a home with you! To accept it click on this message.",
    "sharehome_failure": "Error : The home don't exist.",
    "sharehome_yourself": "Error : You can't share with yourself.",
    "accepthome_success": "The home has been transferred! Run /homes to find it.",
    "accepthome_failure": "Error : The home don't exist.",
    "accepthome_empty": "Error : There is no home to accept.",
    "homelanguage_success": "HOME language changed!",
    "homelanguage_failure": "Error : The language provided is invalid.",
    "version": "1.2"
  },
  "fr": {
    "sethome_success": "Votre maison a été définie !",
    "psethome_success": "Votre maison publique a été définie !",
    "delhome_success": "Votre maison a été supprimée !",
    "delhome_failure": "Erreur : La maison n'existe pas.",
    "pdelhome_success": "Votre maison publique a été supprimée !",
    "pdelhome_failure": "Erreur : La maison publique n'existe pas ou vous n'en êtes pas le propriétaire.",
    "home_success": "Vous avez été téléporté à votre maison !",
    "home_failure": "Erreur : La maison n'existe pas.",
    "phome_success": "Vous avez été téléporté à la maison publique !",
    "phome_failure": "Erreur : La maison publique n'existe pas.",
    "homes_success": "Vos maisons (Vous pouvez cliquer dessus pour vous téléporter) :",
    "homes_failure": "Erreur : Vous n'avez aucune maison.",
    "phomes_success": "Maisons publiques (Vous pouvez cliquer dessus pour vous téléporter) :",
    "phomes_failure": "Erreur : Le serveur ne possède aucune maison publique.",
    "sharehome_success": "%s veut partager une maison avec vous ! Pour l'accepter, cliquez sur ce message.",
    "sharehome_failure": "Erreur : La maison n'existe pas.",
    "sharehome_yourself": "Erreur : Vous ne pouvez pas partager avec vous-même.",
    "accepthome_success": "La maison a été transférée ! Exécutez /homes pour la trouver.",
    "accepthome_failure": "Erreur : La maison n'existe pas.",
    "accepthome_empty": "Erreur : Il n'y a pas de maison à accepter.",
    "homelanguage_success": "La langue de HOME a été modifiée !",
    "homelanguage_failure": "Erreur : La langue fournie est invalide.",
    "version": "1.2"
  }
}
```
