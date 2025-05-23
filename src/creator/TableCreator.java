package creator;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TableCreator {

    private Connection conn;

    public TableCreator() {
        connectionBD();
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

    public void resetAndCreateTables() throws SQLException {
        dropAllTables();
        createAllTables();
        conn.commit();
        System.out.println("All tables reset and created successfully (case-sensitive).");
    }

    public void dropAllTables() {
        System.out.println("Starting to drop tables (case-sensitive)...");
        // Drop in reverse order of creation dependencies
        // Note: The tableName parameter to dropTable should match the exact case used in CREATE TABLE
        dropTable("Location");
        dropTable("Role");
        dropTable("FilmScenariste");
        dropTable("FilmGenre");
        dropTable("FilmPays");
        dropTable("BandeAnnonce");
        dropTable("Copie");
        dropTable("Client");
        dropTable("Forfait");
        dropTable("Employe");
        dropTable("Film");
        dropTable("Utilisateur");
        dropTable("Scenariste");
        dropTable("Personne");
        dropTable("Genre");
        dropTable("PaysProduction");
        dropTable("DomaineCopie");
        dropTable("DomaineForfait");
        dropTable("DomaineCarteCredit");
        System.out.println("Finished dropping tables (case-sensitive).");
    }

    private void dropTable(String tableName) {
        // Enclose table name in double quotes for case-sensitivity
        String sql = "DROP TABLE \"" + tableName + "\" CASCADE CONSTRAINTS";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
            System.out.println("Table \"" + tableName + "\" dropped successfully.");
        } catch (SQLException e) {
            // ORA-00942: table or view does not exist
            if (e.getErrorCode() == 942) {
                System.out.println("Table \"" + tableName + "\" does not exist, skipping drop.");
            } else {
                System.err.println("Error dropping table \"" + tableName + "\": " + e.getMessage());
                // e.printStackTrace(); // Uncomment for detailed error during development
            }
        }
    }

    private void createAllTables() throws SQLException {
        System.out.println("Starting to create tables (case-sensitive)...");
        // Create tables in order of dependency
        createDomaineCarteCreditTable();
        createDomaineForfaitTable();
        createDomaineCopieTable();
        createPaysProductionTable();
        createGenreTable();
        createPersonneTable();
        createScenaristeTable();
        createUtilisateurTable();

        createFilmTable(); // Depends on Realisateur

        createForfaitTable(); // Depends on DomaineForfait
        createEmployeTable(); // Depends on Utilisateur

        createClientTable(); // Depends on Utilisateur, DomaineCarteCredit, Forfait

        createCopieTable(); // Depends on Film, DomaineCopie
        createBandeAnnonceTable(); // Depends on Film
        createFilmPaysTable(); // Depends on Film, PaysProduction
        createFilmGenreTable(); // Depends on Film, Genre
        createFilmScenaristeTable(); // Depends on Film, Scenariste

        createRoleTable(); // Depends on Film, Acteur
        createLocationTable(); // Depends on Client, Copie
        System.out.println("Finished creating tables (case-sensitive).");
    }

    // Domain Tables (Lookup/Reference Tables)
    private void createDomaineCarteCreditTable() throws SQLException {
        String sql = """
            CREATE TABLE "DomaineCarteCredit" (
                "carteCreditType" VARCHAR2(10) PRIMARY KEY
            )
        """;
        executeUpdate(sql, "DomaineCarteCredit");
    }

    private void createDomaineForfaitTable() throws SQLException {
        String sql = """
            CREATE TABLE "DomaineForfait" (
                "type" VARCHAR2(10) PRIMARY KEY
            )
        """;
        executeUpdate(sql, "DomaineForfait");
    }

    private void createDomaineCopieTable() throws SQLException {
        String sql = """
            CREATE TABLE "DomaineCopie" (
                "etat" VARCHAR2(10) PRIMARY KEY
            )
        """;
        executeUpdate(sql, "DomaineCopie");
    }

    private void createPaysProductionTable() throws SQLException {
        String sql = """
            CREATE TABLE "PaysProduction" (
                "nomPays" VARCHAR2(60) PRIMARY KEY
            )
        """;
        executeUpdate(sql, "PaysProduction");
    }

    private void createGenreTable() throws SQLException {
        String sql = """
            CREATE TABLE "Genre" (
                "nomGenre" VARCHAR2(20) PRIMARY KEY
            )
        """;
        executeUpdate(sql, "Genre");
    }

    // Core Entity Tables
    private void createPersonneTable() throws SQLException {
        String sql = """
            CREATE TABLE "Personne" (
                "idPersonne" VARCHAR2(10) PRIMARY KEY,
                "nom" VARCHAR2(255) NOT NULL,
                "dateNaissance" DATE,
                "lieuNaissance" VARCHAR2(255),
                "photo" VARCHAR2(255),
                "biographie" CLOB
            )
        """;
        executeUpdate(sql, "Personne");
    }

    private void createScenaristeTable() throws SQLException {
        String sql = """
            CREATE TABLE "Scenariste" (
                "idScenariste" VARCHAR2(36) PRIMARY KEY,
                "nom" VARCHAR2(255) NOT NULL
            )
        """;
        executeUpdate(sql, "Scenariste");
    }

    private void createUtilisateurTable() throws SQLException {
        String sql = """
            CREATE TABLE "Utilisateur" (
                "idUser" VARCHAR2(10) PRIMARY KEY,
                "prenom" VARCHAR2(255) NOT NULL,
                "nom" VARCHAR2(255) NOT NULL,
                "courriel" VARCHAR2(255) NOT NULL UNIQUE,
                "motDePasse" VARCHAR2(50) NOT NULL,
                "telephone" VARCHAR2(20) NOT NULL,
                "adresse" VARCHAR2(255) NOT NULL,
                "ville" VARCHAR2(255) NOT NULL,
                "province" VARCHAR2(20) NOT NULL,
                "codePostal" VARCHAR2(10) NOT NULL,
                "dateNaissance" DATE NOT NULL
            )
        """;
        executeUpdate(sql, "Utilisateur");
    }

    // Core Media Table
    private void createFilmTable() throws SQLException {
        String sql = """
            CREATE TABLE "Film" (
                "idFilm" VARCHAR2(10) PRIMARY KEY,
                "titre" VARCHAR2(255) NOT NULL,
                "anneeSortie" NUMBER(4) NOT NULL,
                "langue" VARCHAR2(50),
                "dureeFilm" NUMBER NOT NULL,
                "resume" CLOB,
                "affiche" VARCHAR2(255),
                "idRealisateur" VARCHAR2(10) NOT NULL,
                CONSTRAINT "fk_film_realisateur" FOREIGN KEY ("idRealisateur") REFERENCES "Personne"("idPersonne")
            )
        """;
        executeUpdate(sql, "Film");
    }

    // Financial/Subscription Tables
    private void createForfaitTable() throws SQLException {
        String sql = """
            CREATE TABLE "Forfait" (
                "codeForfait" VARCHAR2(10) PRIMARY KEY,
                "coûtMensuel" NUMBER(10, 2) NOT NULL,
                "locationsMax" NUMBER NOT NULL,
                "dureeMaxJours" NUMBER NOT NULL,
                "type" VARCHAR2(10) NOT NULL,
                CONSTRAINT "fk_forfait_domaine" FOREIGN KEY ("type") REFERENCES "DomaineForfait"("type")
            )
        """;
        executeUpdate(sql, "Forfait");
    }

    // User Role Tables
    private void createEmployeTable() throws SQLException {
        String sql = """
            CREATE TABLE "Employe" (
                "idUser" VARCHAR2(10) PRIMARY KEY,
                "matricule" NUMBER NOT NULL UNIQUE,
                CONSTRAINT "fk_employe_utilisateur" FOREIGN KEY ("idUser") REFERENCES "Utilisateur"("idUser") ON DELETE CASCADE
            )
        """;
        executeUpdate(sql, "Employe");
    }

    private void createClientTable() throws SQLException {
        String sql = """
            CREATE TABLE "Client" (
                "idUser" VARCHAR2(10) PRIMARY KEY,
                "carteCreditNumero" VARCHAR2(19) NOT NULL UNIQUE,
                "carteCreditExpMois" NUMBER(2) NOT NULL,
                "carteCreditExpAnnee" NUMBER(4) NOT NULL,
                "carteCreditCVV" NUMBER(4) NOT NULL,
                "carteCreditType" VARCHAR2(10) NOT NULL,
                "codeForfait" VARCHAR2(10) NOT NULL,
                CONSTRAINT "fk_client_utilisateur" FOREIGN KEY ("idUser") REFERENCES "Utilisateur"("idUser") ON DELETE CASCADE,
                CONSTRAINT "fk_client_domaine_carte" FOREIGN KEY ("carteCreditType") REFERENCES "DomaineCarteCredit"("carteCreditType"),
                CONSTRAINT "fk_client_forfait" FOREIGN KEY ("codeForfait") REFERENCES "Forfait"("codeForfait")
            )
        """;
        executeUpdate(sql, "Client");
    }

    // Media Instance and Related Tables
    private void createCopieTable() throws SQLException {
        String sql = """
            CREATE TABLE "Copie" (
                "code" VARCHAR2(255) PRIMARY KEY,
                "idFilm" VARCHAR2(10) NOT NULL,
                "etat" VARCHAR2(10) NOT NULL,
                CONSTRAINT "fk_copie_film" FOREIGN KEY ("idFilm") REFERENCES "Film"("idFilm") ON DELETE CASCADE,
                CONSTRAINT "fk_copie_domaine_etat" FOREIGN KEY ("etat") REFERENCES "DomaineCopie"("etat")
            )
        """;
        executeUpdate(sql, "Copie");
    }

    private void createBandeAnnonceTable() throws SQLException {
        String sql = """
            CREATE TABLE "BandeAnnonce" (
                "idBandeAnnonce" VARCHAR2(36) PRIMARY KEY,
                "url" VARCHAR2(255) NOT NULL,
                "idFilm" VARCHAR2(10) NOT NULL,
                CONSTRAINT "fk_bandeannonce_film" FOREIGN KEY ("idFilm") REFERENCES "Film"("idFilm") ON DELETE CASCADE
            )
        """;
        executeUpdate(sql, "BandeAnnonce");
    }

    // Junction Tables
    private void createFilmPaysTable() throws SQLException {
        String sql = """
            CREATE TABLE "FilmPays" (
                "idFilm" VARCHAR2(10),
                "nomPays" VARCHAR2(60),
                PRIMARY KEY ("idFilm", "nomPays"),
                CONSTRAINT "fk_filmpays_film" FOREIGN KEY ("idFilm") REFERENCES "Film"("idFilm") ON DELETE CASCADE,
                CONSTRAINT "fk_filmpays_pays" FOREIGN KEY ("nomPays") REFERENCES "PaysProduction"("nomPays") ON DELETE CASCADE
            )
        """;
        executeUpdate(sql, "FilmPays");
    }

    private void createFilmGenreTable() throws SQLException {
        String sql = """
            CREATE TABLE "FilmGenre" (
                "idFilm" VARCHAR2(10),
                "nomGenre" VARCHAR2(20),
                PRIMARY KEY ("idFilm", "nomGenre"),
                CONSTRAINT "fk_filmgenre_film" FOREIGN KEY ("idFilm") REFERENCES "Film"("idFilm") ON DELETE CASCADE,
                CONSTRAINT "fk_filmgenre_genre" FOREIGN KEY ("nomGenre") REFERENCES "Genre"("nomGenre") ON DELETE CASCADE
            )
        """;
        executeUpdate(sql, "FilmGenre");
    }

    private void createFilmScenaristeTable() throws SQLException {
        String sql = """
            CREATE TABLE "FilmScenariste" (
                "idFilm" VARCHAR2(10),
                "idScenariste" VARCHAR2(36),
                PRIMARY KEY ("idFilm", "idScenariste"),
                CONSTRAINT "fk_filmscenariste_film" FOREIGN KEY ("idFilm") REFERENCES "Film"("idFilm") ON DELETE CASCADE,
                CONSTRAINT "fk_filmscenariste_scenariste" FOREIGN KEY ("idScenariste") REFERENCES "Scenariste"("idScenariste") ON DELETE CASCADE
            )
        """;
        executeUpdate(sql, "FilmScenariste");
    }

    private void createRoleTable() throws SQLException {
        String sql = """
            CREATE TABLE "Role" (
                "idRole" VARCHAR2(36) PRIMARY KEY,
                "personnage" VARCHAR2(255),
                "idFilm" VARCHAR2(10) NOT NULL,
                "idActeur" VARCHAR2(10) NOT NULL,
                CONSTRAINT "fk_role_film" FOREIGN KEY ("idFilm") REFERENCES "Film"("idFilm") ON DELETE CASCADE,
                CONSTRAINT "fk_role_acteur" FOREIGN KEY ("idActeur") REFERENCES "Personne"("idPersonne") ON DELETE CASCADE            )
        """;
        executeUpdate(sql, "Role");
    }

    // Transactional Table
    private void createLocationTable() throws SQLException {
        String sql = """
            CREATE TABLE "Location" (
                "idLocation" VARCHAR2(10) PRIMARY KEY,
                "dateDebut" DATE NOT NULL,
                "dateFin" DATE NOT NULL,
                "dateRetourEffectif" DATE,
                "idClient" VARCHAR2(10) NOT NULL,
                "idCopie" VARCHAR2(10) NOT NULL,
                CONSTRAINT "fk_location_client" FOREIGN KEY ("idClient") REFERENCES "Client"("idUser"),
                CONSTRAINT "fk_location_copie" FOREIGN KEY ("idCopie") REFERENCES "Copie"("code")
            )
        """;
        executeUpdate(sql, "Location");
    }

    // Helper method to execute SQL updates and print messages
    private void executeUpdate(String sql, String tableName) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
            // Use double quotes for printing the table name to reflect its actual case-sensitive name
            System.out.println("Table \"" + tableName + "\" created successfully.");
        } catch (SQLException e) {
            System.err.println("Error creating table \"" + tableName + "\": " + e.getMessage());
            throw e; // Re-throw to be handled by the caller, potentially rolling back
        }
    }
}
