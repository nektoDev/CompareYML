package ru.nektodev.wikimart.worker;

import ru.nektodev.wikimart.model.Offer;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;

/**
 * Class to compare to files with offer objects. Use StAX because it simple, fast, and not in-memory parsing
 *
 * @author Tsykin V.A.
 *         ts.slawa@gmail.com
 * @date 27.09.15
 */
public class YMLComparator{

    public static final String OFFER_TAG = "offer";
    public static final String ID_ATTRIBUTE = "id";
    public static final String PRICE_TAG = "price";
    public static final String PICTURE_TAG = "picture";
    public static final String NEW_OFFER_MODIFICATOR = "n";
    public static final String MODIFIED_OFFER_MODIFICATOR = "m";

    private final TreeMap<Integer, Offer> offersTree;
    private final BlockingQueue<Offer> picturesQueue;

    private XMLStreamReader oldFileParser;
    private XMLStreamReader newFileParser;

    public YMLComparator(TreeMap<Integer, Offer> offersTree, File oldFile, File newFile, BlockingQueue<Offer> picturesQueue) throws FileNotFoundException, XMLStreamException {
        this.offersTree = offersTree;
        this.picturesQueue = picturesQueue;

        XMLInputFactory factory = XMLInputFactory.newInstance();
        oldFileParser = factory.createXMLStreamReader(new FileInputStream(oldFile.getPath()));
        newFileParser = factory.createXMLStreamReader(new FileInputStream(newFile.getPath()));
    }

    /**
     * Main method. Starts {@link #parseOldFile()} and {@link #parseNewFile()}. Just stop if interrupted.
     * @throws XMLStreamException in case of errors in XML
     * @see #parseNewFile()
     * @see #parseOldFile()
     */
    public void run() throws XMLStreamException {

        try {
            parseOldFile();
            parseNewFile();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Parsing old file, creating {@link Offer} object and put if to TreeMap for comparing with "new file" objects. <br/>
     * Also put object to queue for {@link PictureCheckWorker} for checking picture availability
     * @throws XMLStreamException
     * @throws InterruptedException
     * @see #parseNewFile()
     */
    private void parseOldFile() throws XMLStreamException, InterruptedException {
        while (oldFileParser.hasNext()) {
            int event = oldFileParser.next();

            if (event == XMLStreamConstants.START_ELEMENT) {
                if (oldFileParser.getLocalName().equalsIgnoreCase(OFFER_TAG)) {
                    Offer offer = getOffer(oldFileParser);

                    offersTree.put(offer.getId(), offer);
                    picturesQueue.put(offer);
                }
            }
        }

        oldFileParser.close();
    }

    /**
     * Parse new file and compare Offers with objects in TreeMap. If there is no object with such id, set modifier to "n",
     * add it to TreeMap and queue for picture availability check. <br/>
     * If found this object - compare prices, and if price changes, set modifier to "m"
     * @throws XMLStreamException
     * @throws InterruptedException
     * @see #parseOldFile()
     * @see Offer
     */
    private void parseNewFile() throws XMLStreamException, InterruptedException {

        while (newFileParser.hasNext()) {

            int event = newFileParser.next();

            if (event == XMLStreamConstants.START_ELEMENT) {
                if (newFileParser.getLocalName().equalsIgnoreCase(OFFER_TAG)) {

                    Offer offer = getOffer(newFileParser);

                    Offer oldOffer = offersTree.get(offer.getId());
                    if (oldOffer == null) {
                        offer.getResult().append(NEW_OFFER_MODIFICATOR);
                        offer.setIsFoundedInNewFile(true);

                        offersTree.put(offer.getId(), offer);
                        picturesQueue.put(offer);
                    } else {

                        oldOffer.setIsFoundedInNewFile(true);
                        if (!offer.getPrice().equals(oldOffer.getPrice())) {
                            oldOffer.getResult().append(MODIFIED_OFFER_MODIFICATOR);
                        }
                    }
                }
            }
        }

        newFileParser.close();
    }

    /**
     * Parse with StAX <offer> element and create {@link Offer} object
     * @param parser
     * @return new Offer object
     * @throws XMLStreamException
     */
    private Offer getOffer(XMLStreamReader parser) throws XMLStreamException {
        int event;
        Offer offer = new Offer();

        offer.setId(getId(parser));

        while (true) {
            event = parser.next();
            if (event == XMLStreamConstants.START_ELEMENT && parser.getLocalName().equalsIgnoreCase(PRICE_TAG)) {

                String price = getText(parser, PRICE_TAG);
                offer.setPrice(price);
                continue;
            }
            if (event == XMLStreamConstants.START_ELEMENT && parser.getLocalName().equalsIgnoreCase(PICTURE_TAG)) {
                offer.setPictureUrl(getText(parser, PICTURE_TAG));
                continue;
            }

            if (event == XMLStreamConstants.END_ELEMENT && parser.getLocalName().equalsIgnoreCase(OFFER_TAG)) {
                break;
            }
        }
        return offer;
    }

    /**
     * Parse attributes of offer tag and return value of Id attribute
     * @param parser
     * @return
     */
    private Integer getId(XMLStreamReader parser) {
        for (int i = 0; i < parser.getAttributeCount(); i++) {
            if (ID_ATTRIBUTE.equalsIgnoreCase(parser.getAttributeLocalName(i))) {
                return Integer.valueOf(parser.getAttributeValue(i));
            }
        }
        return null;
    }

    /**
     * Get text from specified tag
     * @param parser
     * @param tag tag to getting it text
     * @return text from tag
     * @throws XMLStreamException
     */
    private String getText(XMLStreamReader parser, String tag) throws XMLStreamException {
        int event;
        while (true) {
            event = parser.next();

            if (event == XMLStreamConstants.CHARACTERS) {
                return parser.getText();
            }

            if (event == XMLStreamConstants.END_ELEMENT && parser.getLocalName().equalsIgnoreCase(tag)) {
                break;
            }
        }
        return "";
    }
}
