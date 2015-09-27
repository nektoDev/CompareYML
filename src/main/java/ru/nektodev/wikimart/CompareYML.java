package ru.nektodev.wikimart;

import com.sun.tools.doclets.formats.html.SourceToHTMLConverter;
import ru.nektodev.wikimart.model.Offer;
import ru.nektodev.wikimart.worker.PictureCheckWorker;
import ru.nektodev.wikimart.worker.YMLComparator;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Test application for Wikimart. Main class. <br/>
 * Application should compare objects from 2 files and show object id's with: <br/>
 * <ul>
 *     <li>Changed price</li>
 *     <li>New object</li>
 *     <li>Removed object</li>
 *     <il>Unavailable picture</il>
 * </ul>
 * <br/>
 * Start parameters: <br/>
 * <ul>
 *     <li>Path to <i>old</i> file <b>required</b></li>
 *     <li>Path to <i>new</i> file <b>required</b></li>
 *     <li>Number of threads to check pictures <b>optional - by default 10</b></li>
 * </ul>
 * <br/>
 * Output format:<br/>
 * id mnpr<br/>
 * <br/>
 * m - modified<br/>
 * n - new<br/>
 * p - unavailable picture<br/>
 * r - removed<br/>
 *
 * @author Tsykin V.A.
 *         ts.slawa@gmail.com
 * @date 27.09.15
 */
public class CompareYML {

    private static final TreeMap<Integer, Offer> offerTree = new TreeMap<>();
    private static final BlockingQueue<Offer> picturesQueue = new LinkedBlockingQueue<>();

    /**
     * Main method. Check arguments and start application
     * @param args <ul><li>Path to <i>old</i> file <b>required</b></li>
     *                 <li>Path to <i>new</i> file <b>required</b></li>
     *                 <li>Number of threads to check pictures <b>optional - by default 10</b></li>
     *              </ul>

     */
    public static void main(String[] args) {
        //Validate args
        if (args.length == 0 || args.length < 2) {
            System.out.println("To start application you should pass parameters:\n" +
                    "1. path to old file\n" +
                    "2. path to new file\n" +
                    "3. optional: number of threads to check pictures state (default: 10)" );
            System.exit(1);
        }

        File oldFile = new File(args[0]);
        File newFile = new File(args[1]);
        Integer threadCounts = 10;

        if (args.length == 3) {
            try {
                threadCounts = Integer.valueOf(args[2]);
            } catch (NumberFormatException e) {
                System.out.println("Incorrect number of threads!");
                System.exit(1);
            }
        }
        if (!oldFile.exists()) {
            System.out.println("Incorrect path to old file!");
            System.exit(1);
        }
        if (!newFile.exists()) {
            System.out.println("Incorrect path to old file!");
            System.exit(1);
        }

        System.out.println("Start application...");
        startApplication(oldFile, newFile, threadCounts);

        System.out.println("End application...");
        System.exit(0);
    }

    /**
     * Start application. Initialize {@link YMLComparator} and {@link PictureCheckWorker} thread pool. <br/>
     * After endind of parsing wait while all pictures will be checked and call {@link #printResult()}
     * @param oldFile <i>old</i> file to compare objects
     * @param newFile <i>new</i> file to compare objects
     * @param threadCounts number of threads to check pictures
     * @see Offer
     * @see PictureCheckWorker
     * @see YMLComparator
     */
    private static void startApplication(File oldFile, File newFile, Integer threadCounts) {
        ExecutorService service = Executors.newFixedThreadPool(threadCounts);

        for (int i = 0; i<threadCounts; i++ ) {
            PictureCheckWorker w = new PictureCheckWorker(picturesQueue);
            service.submit(w);
        }

        try {
            YMLComparator comparator = new YMLComparator(offerTree, oldFile, newFile, picturesQueue);
            comparator.run();

        } catch (FileNotFoundException e) {
            System.out.println("File disappear!");
            e.printStackTrace();
        } catch (XMLStreamException e) {
            System.out.println("Error while parsing XML");
            e.printStackTrace();
        }

        //Check that all pictures was checked
        while (picturesQueue.size() > 0) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        service.shutdownNow();

        printResult();
    }

    /**
     * Prints result of file comparing. Prints objects that have modificators (m/p/r/n)
     */
    private static void printResult() {
        for (Offer offer : offerTree.values()) {
            if (!offer.isFoundedInNewFile()) {
                offer.getResult().append("r");
            }

            if (offer.getResult().length() != 0) {
                System.out.println(offer.getId() + " " + offer.getResult());
            }
        }
    }

}
