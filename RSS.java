import org.w3c.dom.Element;
import java.net.URL;
import java.io.*;
import java.util.Scanner;
import java.util.ArrayList;
import java.net.MalformedURLException;
import org.jsoup.nodes.Element;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Document;

public class RSS {
    private static Scanner scanner = new Scanner(System.in);

    public static void update() {

        System.out.println("Website Titles:");
        System.out.println("[0] All website");

        ArrayList<String> titles = getTitle("db.txt");

        for (int i = 1; i < titles.size(); i++) {
            System.out.println("[" + i + "] " + titles.get(i-1));
        }
        System.out.println("[-1] exit");

        Scanner localScanner = new Scanner(System.in);
        System.out.println("Enter the number of the title you want to view:");
        int choice = Integer.parseInt(localScanner.nextLine());

        switch (choice) {
            case -1:
                System.out.println("");
                break;
            case 0:
                RSSALL();
                break;
            default:
                if (choice >= 1 && choice < titles.size() + 1) {
                    RssContent(findRssLinkWithName(titles.get(choice - 1)));
                } else {
                    System.out.println("Invalid choice!");
                }
                break;
        }

    }

    public static void add() {
        System.out.println("Enter URL details in the format:");
        String url = scanner.nextLine();

        try (BufferedReader reader = new BufferedReader(new FileReader("db.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(url)) {
                    System.out.println("This URL "+url+" already exists in the file.");
                    return;
                }
            }
        } catch (IOException e) {
            System.out.println("An error occurred while reading the file.");
            e.printStackTrace();
        }

        String[] siteInfo = extractSiteInfo(url);
        String websiteName = siteInfo[0];
        String rssUrl = siteInfo[1];

        String output = websiteName + ";" + url+ ";" + rssUrl;

        if (rssUrl.length()<2) {
            System.out.println("RSS URL not found!");
            return;
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("db.txt", true))) {
            writer.write(output);
            writer.newLine();
            System.out.println("URL "+url+" added successfully!");
        } catch (IOException e) {
            System.out.println("An error occurred while writing to the file.");
            e.printStackTrace();
        }
    }

    public static void remove() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Please input the URL you want to remove:");
        String siteName = scanner.nextLine();

        File inputFile = new File("db.txt");
        File tempFile = new File("tempdb.txt");

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
             BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {

            String line;
            boolean found = false;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(";");
                if (parts.length >= 3) {
                    String name = parts[1].trim();
                    if (name.equals(siteName)) {
                        found = true;
                        continue;
                    }
                    writer.write(line + System.lineSeparator());
                }
            }

            if (!found) {
                tempFile.delete();
                System.out.println("The site was not found in the database.");
            }

            if (found) {
                System.out.println("The site " + siteName + " has been successfully removed from the database.");
            }

        } catch (IOException e) {
            System.out.println("An error occurred while processing the file: " + e.getMessage());
        }

        if (!inputFile.delete()) {
            System.out.println("Unable to delete the original file.");
            return;
        }
        if (!tempFile.renameTo(inputFile)) {
            System.out.println("Unable to rename the temporary file.");
        }
    }

    public static String[] extractSiteInfo(String websiteUrl) {
        String[] siteInfo = new String[2];

        try {
            @SuppressWarnings("deprecation")
            URL url = new URL(websiteUrl);
            String host = url.getHost();
            siteInfo[0] = host;

            Document doc = Jsoup.connect(websiteUrl).get();
            String rssUrl = doc.select("link[type='application/rss+xml']").attr("abs:href");
            siteInfo[1] = rssUrl;
        } catch (MalformedURLException e) {
            System.out.println("Invalid URL format!");
            return new String[]{"", ""};
        } catch (IOException e) {
            System.out.println("An error occurred while connecting to the website: " + e.getMessage());
            return new String[]{"", ""};
        }

        return siteInfo;
    }

    public static void RSSALL() {
        try (BufferedReader reader = new BufferedReader(new FileReader("db.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(";");
                if (parts.length >= 3) {
                    String rssUrl = parts[2];
                    RssContent(rssUrl);
                }
            }
        } catch (IOException e) {
            System.out.println("An error occurred while reading db.txt: " + e.getMessage());
        }
    }

    public static String findRssLinkWithName(String siteName) {
        try (BufferedReader reader = new BufferedReader(new FileReader("db.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(";");
                if (parts.length >= 3) {
                    String name = parts[0];
                    String rssUrl = parts[2];
                    if (name.equals(siteName)) {
                        return rssUrl ;
                    }
                }
            }
            System.out.println("RSS for " + siteName + " not found in db.txt");
        } catch (IOException e) {
            System.out.println("An error occurred while reading db.txt: " + e.getMessage());
        }
        return "";
    }

    public static void RssContent(String rssUrl) {
        try {
            Document doc = Jsoup.connect(rssUrl).get();
            Elements itemElements = doc.select("item");

            for (int i = 0; i < 5 && i < itemElements.size(); ++i) {
                Element itemElement = itemElements.get(i);
                System.out.println("Title: " + getElementTextContent(itemElement, "title"));
                System.out.println("Link: " + getElementTextContent(itemElement, "link"));
                System.out.println("Description: " + getElementTextContent(itemElement, "description"));
                System.out.println();
            }
        } catch (IOException e) {
            System.out.println("Error in retrieving RSS content for " + rssUrl + ": " + e.getMessage());
        }
    }

    public static String getElementTextContent(Element parentElement, String tagName) {
        Element element = parentElement.selectFirst(tagName);
        if (element != null) {
            return element.text();
        }
        return "";
    }

    public static ArrayList<String> getTitle(String filename) {
        ArrayList<String> titles = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(";");
                if (parts.length >= 1) {
                    titles.add(parts[0]);
                }
            }
        } catch (IOException e) {
            System.out.println("An error occurred while reading the file.");
            e.printStackTrace();
        }
        return titles;
    }

    public static void main(String[] args) {
        int num;

        do {
            System.out.println("Welcome to RSS Reader!");
            System.out.println("Type a valid number for your desired action:");
            System.out.println("[1] Show updates");
            System.out.println("[2] Add URL");
            System.out.println("[3] Remove URL");
            System.out.println("[4] Exit");
            num = Integer.parseInt(scanner.nextLine());

            if (num == 1) {
                update();
            } else if (num == 2) {
                add();
            } else if (num == 3) {
                remove();
            } else {
                if (num != 4) {
                    System.out.println("Invalid num! Please enter a valid number.");
                }
            }
        } while (num != 4);
    }
}