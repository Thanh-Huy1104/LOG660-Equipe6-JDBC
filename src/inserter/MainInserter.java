package inserter;

import creator.MainCreator;
import creator.TableCreator;

import java.sql.SQLException;

public class MainInserter {
    public static void main(String[] args) throws SQLException {
        System.out.println("Ready to insert data!");
        long startTime = System.currentTimeMillis();
        TableCreator tableCreator = new TableCreator();
        tableCreator.resetAndCreateTables();
        LectureBD lectureBD = new LectureBD();

        try {
            lectureBD.lectureClients("src/data/clients_latin1.xml");
            lectureBD.lecturePersonnes("src/data/personnes_latin1.xml");
            lectureBD.lectureFilms("src/data/films_latin1.xml");
        } catch (Exception  e) {
            System.err.println("An error occurred: " + e.getMessage());
            e.printStackTrace();
        } finally {
            lectureBD.closeAll();
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        System.out.println("Total execution time: " + duration + " milliseconds");
    }
}
