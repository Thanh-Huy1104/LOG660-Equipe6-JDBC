package inserter;

import java.io.FileInputStream;
import java.io.IOException;

import java.io.InputStream;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Random;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class LectureBD {
    private Connection conn;
    private Random random = new Random();
    private static final int BATCH_SIZE = 100;

    private PreparedStatement psPersonne;
    private PreparedStatement psUtilisateur;
    private PreparedStatement psClient;
    private PreparedStatement psFilm;
    private PreparedStatement psRealisateur;
    private PreparedStatement psActeur;
    private PreparedStatement psDomaineCarteCreditMerge;
    private PreparedStatement psDomaineForfaitMerge;
    private PreparedStatement psForfaitMerge;
    private PreparedStatement psDomaineCopieMerge;
    private PreparedStatement psPaysProductionMerge;
    private PreparedStatement psGenreMerge;
    private PreparedStatement psScenaristeMerge;
    private PreparedStatement psFilmPays;
    private PreparedStatement psFilmGenre;
    private PreparedStatement psFilmScenariste;
    private PreparedStatement psRole;
    private PreparedStatement psBandeAnnonce;
    private PreparedStatement psCopie;

    // Counters for batch execution
    private int personneBatchCount = 0;
    private int utilisateurBatchCount = 0;
    private int clientBatchCount = 0;
    private int filmBatchCount = 0;
    private int realisateurBatchCount = 0;
    private int acteurBatchCount = 0;
    private int relatedFilmDataBatchCount = 0;


    public class Role {
        public Role(int i, String n, String p) {
            id = i;
            nom = n;
            personnage = p;
        }

        protected int id;
        protected String nom;
        protected String personnage;
    }

    public LectureBD() {
        connectionBD();
        initializePreparedStatements();
    }

    private void initializePreparedStatements() {
        try {
            psPersonne = conn.prepareStatement("INSERT INTO \"Personne\" (\"idPersonne\", \"nom\", \"dateNaissance\", \"lieuNaissance\", \"photo\", \"biographie\") VALUES (?, ?, TO_DATE(?, 'YYYY-MM-DD'), ?, ?, ?)");
            psUtilisateur = conn.prepareStatement("INSERT INTO \"Utilisateur\" (\"idUser\", \"prenom\", \"nom\", \"courriel\", \"motDePasse\", \"telephone\", \"adresse\", \"ville\", \"province\", \"codePostal\", \"dateNaissance\") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TO_DATE(?, 'YYYY-MM-DD'))");
            psClient = conn.prepareStatement("INSERT INTO \"Client\" (\"idUser\", \"carteCreditNumero\", \"carteCreditExpMois\", \"carteCreditExpAnnee\", \"carteCreditCVV\", \"carteCreditType\", \"codeForfait\") VALUES (?, ?, ?, ?, ?, ?, ?)");
            psFilm = conn.prepareStatement("INSERT INTO \"Film\" (\"idFilm\", \"titre\", \"anneeSortie\", \"langue\", \"dureeFilm\", \"resume\", \"affiche\", \"idRealisateur\") VALUES (?, ?, ?, ?, ?, ?, ?, ?)");

            psDomaineCarteCreditMerge = conn.prepareStatement("MERGE INTO \"DomaineCarteCredit\" target USING (SELECT ? AS type FROM dual) source ON (target.\"carteCreditType\" = source.type) WHEN NOT MATCHED THEN INSERT (\"carteCreditType\") VALUES (source.type)");
            psDomaineForfaitMerge = conn.prepareStatement("MERGE INTO \"DomaineForfait\" target USING (SELECT ? AS type FROM dual) source ON (target.\"type\" = source.type) WHEN NOT MATCHED THEN INSERT (\"type\") VALUES (source.type)");
            psForfaitMerge = conn.prepareStatement("MERGE INTO \"Forfait\" target USING (SELECT ? AS code, ? AS type FROM dual) source ON (target.\"codeForfait\" = source.code) WHEN NOT MATCHED THEN INSERT (\"codeForfait\", \"coûtMensuel\", \"locationsMax\", \"dureeMaxJours\", \"type\") VALUES (source.code, 0.00, 0, 0, source.type)");
            psDomaineCopieMerge = conn.prepareStatement("MERGE INTO \"DomaineCopie\" target USING (SELECT ? AS etat FROM dual) source ON (target.\"etat\" = source.etat) WHEN NOT MATCHED THEN INSERT (\"etat\") VALUES (source.etat)");
            psPaysProductionMerge = conn.prepareStatement("MERGE INTO \"PaysProduction\" target USING (SELECT ? AS nom FROM dual) source ON (target.\"nomPays\" = source.nom) WHEN NOT MATCHED THEN INSERT (\"nomPays\") VALUES (source.nom)");
            psGenreMerge = conn.prepareStatement("MERGE INTO \"Genre\" target USING (SELECT ? AS nom FROM dual) source ON (target.\"nomGenre\" = source.nom) WHEN NOT MATCHED THEN INSERT (\"nomGenre\") VALUES (source.nom)");
            psScenaristeMerge = conn.prepareStatement("MERGE INTO \"Scenariste\" target USING (SELECT ? AS id, ? AS nom FROM dual) source ON (target.\"idScenariste\" = source.id) WHEN NOT MATCHED THEN INSERT (\"idScenariste\", \"nom\") VALUES (source.id, source.nom)");

            psFilmPays = conn.prepareStatement("INSERT INTO \"FilmPays\" (\"idFilm\", \"nomPays\") VALUES (?, ?)");
            psFilmGenre = conn.prepareStatement("INSERT INTO \"FilmGenre\" (\"idFilm\", \"nomGenre\") VALUES (?, ?)");
            psFilmScenariste = conn.prepareStatement("INSERT INTO \"FilmScenariste\" (\"idFilm\", \"idScenariste\") VALUES (?, ?)");
            psRole = conn.prepareStatement("INSERT INTO \"Role\" (\"idRole\", \"personnage\", \"idFilm\", \"idActeur\") VALUES (?, ?, ?, ?)");
            psBandeAnnonce = conn.prepareStatement("INSERT INTO \"BandeAnnonce\" (\"idBandeAnnonce\", \"url\", \"idFilm\") VALUES (?, ?, ?)");
            psCopie = conn.prepareStatement("INSERT INTO \"Copie\" (\"code\", \"idFilm\", \"etat\") VALUES (?, ?, ?)");

        } catch (SQLException e) {
            System.err.println("Erreur initialisation des PreparedStatement: " + e.getMessage());
            e.printStackTrace();
            closeConnection();
            System.exit(1);
        }
    }


    public void lecturePersonnes(String nomFichier) {
        int count = 0;
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            InputStream is = new FileInputStream(nomFichier);
            parser.setInput(is, null);

            int eventType = parser.getEventType();
            String tag = null, nom = null, anniversaire = null, lieu = null, photo = null, bio = null;
            int id = -1;

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    tag = parser.getName();
                    if (tag.equals("personne") && parser.getAttributeCount() == 1)
                        id = Integer.parseInt(parser.getAttributeValue(0));
                } else if (eventType == XmlPullParser.END_TAG) {
                    tag = null;
                    if (parser.getName().equals("personne") && id >= 0) {
                        insertionPersonne(Integer.toString(id), nom, anniversaire, lieu, photo, bio);
                        count++;
                        personneBatchCount++;
                        if (personneBatchCount % BATCH_SIZE == 0) {
                            psPersonne.executeBatch();
                            System.out.println("Batch of " + BATCH_SIZE + " personnes inserted.");
                        }

                        id = -1;
                        nom = null;
                        anniversaire = null;
                        lieu = null;
                        photo = null;
                        bio = null;
                    }
                } else if (eventType == XmlPullParser.TEXT && id >= 0) {
                    if (tag != null) {
                        String text = parser.getText();
                        if (tag.equals("nom")) nom = text;
                        else if (tag.equals("anniversaire")) anniversaire = text;
                        else if (tag.equals("lieu")) lieu = text;
                        else if (tag.equals("photo")) photo = text;
                        else if (tag.equals("bio")) bio = text;
                    }
                }
                eventType = parser.next();
            }
            if (personneBatchCount % BATCH_SIZE != 0 && personneBatchCount > 0) {
                psPersonne.executeBatch(); // Execute remaining batch
                System.out.println("Final batch of " + (personneBatchCount % BATCH_SIZE) + " personnes inserted.");
            }
            conn.commit();
            System.out.println("Total personnes inserted: " + count);
        } catch (Exception e) {
            System.out.println("Exception while parsing " + nomFichier + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void lectureFilms(String nomFichier) {
        int filmCount = 0;
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();

            InputStream is = new FileInputStream(nomFichier);
            parser.setInput(is, null);

            int eventType = parser.getEventType();

            String tag = null, titre = null, langue = null, poster = null, roleNom = null,
                    rolePersonnage = null, realisateurNom = null, resume = null;

            ArrayList<String> pays = new ArrayList<String>();
            ArrayList<String> genres = new ArrayList<String>();
            ArrayList<String> scenaristes = new ArrayList<String>();
            ArrayList<Role> roles = new ArrayList<Role>();
            ArrayList<String> annonces = new ArrayList<String>();

            int id = -1, annee = -1, duree = -1, roleId = -1, realisateurId = -1;

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    tag = parser.getName();
                    if (tag.equals("film") && parser.getAttributeCount() == 1)
                        id = Integer.parseInt(parser.getAttributeValue(0));
                    else if (tag.equals("realisateur") && parser.getAttributeCount() == 1)
                        realisateurId = Integer.parseInt(parser.getAttributeValue(0));
                    else if (tag.equals("acteur") && parser.getAttributeCount() == 1)
                        roleId = Integer.parseInt(parser.getAttributeValue(0));
                } else if (eventType == XmlPullParser.END_TAG) {
                    String currentTag = parser.getName();
                    tag = null;

                    if (currentTag.equals("film") && id >= 0) {
                        insertionFilm(Integer.toString(id), titre, annee, pays, langue,
                                duree, resume, genres, realisateurNom,
                                realisateurId, scenaristes,
                                roles, poster, annonces);
                        filmCount++;
                        filmBatchCount++;
                        if (filmBatchCount % BATCH_SIZE == 0) {
                            executeFilmBatches();
                            System.out.println("Batch of " + BATCH_SIZE + " films inserted.");
                        }

                        id = -1;
                        annee = -1;
                        duree = -1;
                        titre = null;
                        langue = null;
                        poster = null;
                        resume = null;
                        realisateurNom = null;
                        realisateurId = -1;
                        pays.clear();
                        genres.clear();
                        scenaristes.clear();
                        roles.clear();
                        annonces.clear();

                    }
                    if (parser.getName().equals("role") && roleId >= 0) {
                        roles.add(new Role(roleId, roleNom, rolePersonnage));
                        roleId = -1;
                        roleNom = null;
                        rolePersonnage = null;
                    }
                } else if (eventType == XmlPullParser.TEXT && id >= 0) {
                    if (tag != null) {
                        String text = parser.getText();
                        if (tag.equals("titre")) titre = text;
                        else if (tag.equals("annee")) annee = Integer.parseInt(text);
                        else if (tag.equals("pays")) pays.add(text);
                        else if (tag.equals("langue")) langue = text;
                        else if (tag.equals("duree")) duree = Integer.parseInt(text);
                        else if (tag.equals("resume")) resume = text;
                        else if (tag.equals("genre")) genres.add(text);
                        else if (tag.equals("realisateur")) realisateurNom = text; // Name, ID is from attribute
                        else if (tag.equals("scenariste")) scenaristes.add(text);
                        else if (tag.equals("acteur")) roleNom = text; // Name, ID is from attribute
                        else if (tag.equals("personnage")) rolePersonnage = text;
                        else if (tag.equals("poster")) poster = text;
                        else if (tag.equals("annonce")) annonces.add(text);
                    }
                }
                eventType = parser.next();
            }
            if (filmBatchCount % BATCH_SIZE != 0 && filmBatchCount > 0) {
                executeFilmBatches();
                System.out.println("Final batch of " + (filmBatchCount % BATCH_SIZE) + " films inserted.");
            }
            conn.commit();
            System.out.println("Total films inserted: " + filmCount);
        } catch (Exception e) {
            System.out.println("Exception while parsing " + nomFichier + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void lectureClients(String nomFichier) {
        int count = 0;
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            InputStream is = new FileInputStream(nomFichier);
            parser.setInput(is, null);

            int eventType = parser.getEventType();

            String tag = null, nomFamille = null, prenom = null, courriel = null, tel = null,
                    anniv = null, adresse = null, ville = null, province = null, codePostal = null,
                    carte = null, noCarte = null, motDePasse = null, forfait = null;

            int id = -1, expMois = -1, expAnnee = -1, noCivique = 0;

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    tag = parser.getName();
                    if (tag.equals("client") && parser.getAttributeCount() == 1)
                        id = Integer.parseInt(parser.getAttributeValue(0));
                } else if (eventType == XmlPullParser.END_TAG) {
                    tag = null;
                    if (parser.getName().equals("client") && id >= 0) {
                        insertionClient(Integer.toString(id), nomFamille, prenom, courriel, tel,
                                anniv, adresse, ville, province,
                                codePostal, carte, noCarte,
                                expMois, expAnnee, motDePasse, forfait);
                        count++;
                        utilisateurBatchCount++;

                        if (utilisateurBatchCount % BATCH_SIZE == 0) {
                            executeClientBatches();
                            System.out.println("Batch of " + BATCH_SIZE + " clients inserted.");
                        }

                        id = -1;
                        nomFamille = null;
                        prenom = null;
                        courriel = null;
                        tel = null;
                        anniv = null;
                        adresse = null;
                        ville = null;
                        province = null;
                        codePostal = null;
                        carte = null;
                        noCarte = null;
                        motDePasse = null;
                        forfait = null;
                        expMois = -1;
                        expAnnee = -1;
                    }
                } else if (eventType == XmlPullParser.TEXT && id >= 0) {
                    if (tag != null) {
                        String text = parser.getText();
                        if (text != null) text = text.trim();
                        if (tag.equals("nom-famille")) nomFamille = text;
                        else if (tag.equals("prenom")) prenom = text;
                        else if (tag.equals("courriel")) courriel = text;
                        else if (tag.equals("tel")) tel = text;
                        else if (tag.equals("anniversaire")) anniv = text;
                        else if (tag.equals("adresse")) adresse = text; // Store full address
                        else if (tag.equals("ville")) ville = text;
                        else if (tag.equals("province")) province = text;
                        else if (tag.equals("code-postal")) codePostal = text;
                        else if (tag.equals("carte")) carte = text;
                        else if (tag.equals("no")) noCarte = text;
                        else if (tag.equals("exp-mois") && text != null && !text.isEmpty())
                            expMois = Integer.parseInt(text);
                        else if (tag.equals("exp-annee") && text != null && !text.isEmpty())
                            expAnnee = Integer.parseInt(text);
                        else if (tag.equals("mot-de-passe")) motDePasse = text;
                        else if (tag.equals("forfait")) forfait = text;
                    }
                }
                eventType = parser.next();
            }
            if (utilisateurBatchCount > 0) {
                executeClientBatches();
                System.out.println("Final batch of " + (utilisateurBatchCount % BATCH_SIZE) + " clients inserted.");
                utilisateurBatchCount = 0;
            }

            conn.commit();
            System.out.println("Total clients inserted: " + count);
        } catch (Exception e) {
            System.out.println("Exception while parsing " + nomFichier + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void insertionPersonne(String id, String nom, String anniv, String lieu, String photo, String bio) throws SQLException {
        psPersonne.setString(1, id);
        psPersonne.setString(2, nom);
        psPersonne.setString(3, anniv);
        psPersonne.setString(4, lieu);
        psPersonne.setString(5, photo);
        psPersonne.setString(6, bio);
        psPersonne.addBatch();
    }

    private void insertionFilm(String id, String titre, int annee,
                               ArrayList<String> pays, String langue, int duree, String resume,
                               ArrayList<String> genres, String realisateurNom, int realisateurId,
                               ArrayList<String> scenaristes,
                               ArrayList<Role> roles, String poster,
                               ArrayList<String> annonces) throws SQLException {
        psFilm.setString(1, id);
        psFilm.setString(2, titre);
        psFilm.setInt(3, annee);
        psFilm.setString(4, langue);
        psFilm.setInt(5, duree);
        psFilm.setString(6, resume);
        psFilm.setString(7, poster);
        psFilm.setInt(8, realisateurId);
        psFilm.addBatch();

        for (String paysNom : pays) {
            psPaysProductionMerge.setString(1, paysNom);
            psPaysProductionMerge.addBatch();

            psFilmPays.setString(1, id);
            psFilmPays.setString(2, paysNom);
            psFilmPays.addBatch();
        }

        // ??
        for (String genre : genres) {
            psGenreMerge.setString(1, genre);
            psGenreMerge.addBatch();

            psFilmGenre.setString(1, id);
            psFilmGenre.setString(2, genre);
            psFilmGenre.addBatch();
        }

        for (String scenariste : scenaristes) {
            //Generated id for scenariste
            String idScenariste = Integer.toString(random.nextInt(1000000));
            psScenaristeMerge.setString(1, idScenariste);

            psScenaristeMerge.setString(2, scenariste);
            psScenaristeMerge.addBatch();

            psFilmScenariste.setString(1, id);
            psFilmScenariste.setString(2, scenariste);
            psFilmScenariste.addBatch();
        }

        for (Role role : roles) {
            String idRole = Integer.toString(random.nextInt(1000000));
            psRole.setString(1, idRole);
            psRole.setString(2, role.personnage);
            psRole.setString(3, id);

            // role.id is the actor's ID
            psRole.setString(4, Integer.toString(role.id));
            psRole.addBatch();
        }

        for (String annonce : annonces) {
            String idAnnonce = Integer.toString(random.nextInt(1000000));
            psBandeAnnonce.setString(1, idAnnonce);
            psBandeAnnonce.setString(2, annonce);
            psBandeAnnonce.setString(3, id);
            psBandeAnnonce.addBatch();
        }

        int nombreCopie = random.nextInt(100) + 1;
        String etat = "Disponible";
        psDomaineCopieMerge.setString(1, etat);
        psDomaineCopieMerge.addBatch();

        for (int i = 0; i < nombreCopie; i++) {
            String codeCopie = "CP_" + String.format("%03d", i + 1);
            psCopie.setString(1, codeCopie);
            psCopie.setString(2, id);
            psCopie.setString(3, etat);
            psCopie.addBatch();
        }

    }

    private void insertionClient(String id, String nomFamille, String prenom,
                                 String courriel, String tel, String anniv,
                                 String adresse, String ville, String province,
                                 String codePostal, String carte, String noCarte,
                                 int expMois, int expAnnee, String motDePasse,
                                 String forfait) throws SQLException {
        psUtilisateur.setString(1, id);
        psUtilisateur.setString(2, prenom);
        psUtilisateur.setString(3, nomFamille);
        psUtilisateur.setString(4, courriel);
        psUtilisateur.setString(5, motDePasse);
        psUtilisateur.setString(6, tel);
        psUtilisateur.setString(7, adresse);
        psUtilisateur.setString(8, ville);
        psUtilisateur.setString(9, province);
        psUtilisateur.setString(10, codePostal);
        psUtilisateur.setString(11, anniv);
        psUtilisateur.addBatch();

        // Merge for credit card type
        psDomaineCarteCreditMerge.setString(1, carte);
        psDomaineCarteCreditMerge.addBatch();

        // Merge for forfait
        psDomaineForfaitMerge.setString(1, forfait);
        psDomaineForfaitMerge.addBatch();

        // Merge for forfait with default values
        psForfaitMerge.setString(1, forfait);
        psForfaitMerge.setString(2, forfait); // Assuming type is same as code
        psForfaitMerge.addBatch();

        psClient.setString(1, id);
        psClient.setString(2, noCarte);
        psClient.setInt(3, expMois);
        psClient.setInt(4, expAnnee);

        int cvv = random.nextInt(10000);
        psClient.setInt(5, cvv);
        psClient.setString(6, carte);
        psClient.setString(7, forfait);
        psClient.addBatch();
    }

    private void connectionBD() {
        try {
            Class.forName("oracle.jdbc.OracleDriver");
            conn = DriverManager.getConnection(
                    "jdbc:oracle:thin:@//bdlog660.ens.ad.etsmtl.ca:1521/ORCLPDB.ens.ad.etsmtl.ca",
                    "EQUIPE206",
                    "NulxJFxU"
            );
            conn.setAutoCommit(false);
            System.out.println("Connexion réussie à la base de données.");
        } catch (Exception e) {
            System.err.println("Erreur de connexion à la BD: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void closeAll() {
        System.out.println("Fermeture des ressources JDBC...");
        try {
            if (psPersonne != null) psPersonne.close();
            if (psUtilisateur != null) psUtilisateur.close();
            if (psClient != null) psClient.close();
            if (psFilm != null) psFilm.close();
            if (psDomaineCarteCreditMerge != null) psDomaineCarteCreditMerge.close();
            if (psDomaineForfaitMerge != null) psDomaineForfaitMerge.close();
            if (psForfaitMerge != null) psForfaitMerge.close();
            if (psDomaineCopieMerge != null) psDomaineCopieMerge.close();
            if (psPaysProductionMerge != null) psPaysProductionMerge.close();
            if (psGenreMerge != null) psGenreMerge.close();
            if (psScenaristeMerge != null) psScenaristeMerge.close();
            if (psFilmPays != null) psFilmPays.close();
            if (psFilmGenre != null) psFilmGenre.close();
            if (psFilmScenariste != null) psFilmScenariste.close();
            if (psRole != null) psRole.close();
            if (psBandeAnnonce != null) psBandeAnnonce.close();
            if (psCopie != null) psCopie.close();

            closeConnection();
        } catch (SQLException e) {
            System.err.println("Erreur lors de la fermeture des PreparedStatement: " + e.getMessage());
        }
    }

    private void closeConnection() {
        if (conn != null) {
            try {
                if (!conn.getAutoCommit()) {
                    conn.rollback(); // Rollback if not auto-commit
                    System.out.println("Transaction annulée.");
                }
                conn.setAutoCommit(true);
                conn.close();
                System.out.println("Connexion à la base de données fermée.");
            } catch (SQLException e) {
                System.err.println("Erreur lors de la fermeture de la connexion BD: " + e.getMessage());
            }
        }
    }

    private void executeFilmBatches() throws SQLException {
        if (filmBatchCount > 0) {
            psFilm.executeBatch();
            psPaysProductionMerge.executeBatch();
            psFilmPays.executeBatch();
            psGenreMerge.executeBatch();
            psFilmGenre.executeBatch();
            psScenaristeMerge.executeBatch();
            psFilmScenariste.executeBatch();
            psRole.executeBatch();
            psBandeAnnonce.executeBatch();
            psDomaineCopieMerge.executeBatch();
            psCopie.executeBatch();
        }
    }

    private void executeClientBatches() throws SQLException {
        if (utilisateurBatchCount > 0) { // Check if there's anything to execute for Utilisateur
            psUtilisateur.executeBatch();

            if (psDomaineCarteCreditMerge != null) {
                psDomaineCarteCreditMerge.executeBatch();
            }
            if (psDomaineForfaitMerge != null) {
                psDomaineForfaitMerge.executeBatch();
            }
            if (psForfaitMerge != null) {
                psForfaitMerge.executeBatch();
            }

            if (psClient != null) {
                psClient.executeBatch();
            }
        }
    }


    public static void main(String[] args) {
        LectureBD lecture = new LectureBD();

        lecture.lecturePersonnes(args[0]);
        lecture.lectureFilms(args[1]);
        lecture.lectureClients(args[2]);
    }
}
