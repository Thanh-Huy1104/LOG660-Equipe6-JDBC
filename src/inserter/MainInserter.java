package inserter;

public class MainInserter {
    public static void main(String[] args) {
        System.out.println("Ready to insert data!");
        LectureBD lectureBD = new LectureBD();
        long startTime = System.currentTimeMillis();

        try {
            lectureBD.lectureClients("src/data/clients_latin1.xml");
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
