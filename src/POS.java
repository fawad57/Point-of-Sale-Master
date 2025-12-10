
import java.io.*;
import java.util.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class POS extends PointOfSale {
  public POS() {
  };

  protected void rewriteTempFileExcludingItem(Integer itemIdToRemove,
      String secondHeaderLine) throws IOException {

    String tempPath = "../Database/newTemp.txt";
    File tempFileTarget = new File(tempPath);

    try (BufferedReader reader = new BufferedReader(new FileReader(tempFile));
        BufferedWriter writer = new BufferedWriter(new FileWriter(tempFileTarget))) {

      // First header line: transaction type (Sale / Rental / Return)
      String type = reader.readLine();
      if (type != null) {
        writer.write(type);
        writer.newLine();
      }

      // Optional second header line for rental/return
      if (secondHeaderLine != null) {
        writer.write(secondHeaderLine);
        writer.newLine();
      } else {
        // Consume existing possible header
        reader.mark(1024);
        String maybeSecondLine = reader.readLine();

        if (maybeSecondLine != null && !maybeSecondLine.isEmpty()
            && !maybeSecondLine.matches("\\d+ \\d+")) {
          writer.write(maybeSecondLine);
          writer.newLine();
        } else if (maybeSecondLine != null) {
          reader.reset();
        }
      }

      // Rewrite cart items except removed one
      for (Item item : transactionItem) {
        if (itemIdToRemove != null && item.getItemID() == itemIdToRemove)
          continue;

        writer.write(item.getItemID() + " " + item.getAmount());
        writer.newLine();
      }
    }

    File original = new File(tempFile);
    if (!original.delete()) {
      System.out.println("Unable to delete original temp file");
    }
    if (!tempFileTarget.renameTo(original)) {
      System.out.println("Unable to rename temp file");
    }
  }

  @Override
  public void deleteTempItem(int id) {
    try {
      rewriteTempFileExcludingItem(id, null); // Sale has no phone header
    } catch (IOException e) {
      System.out.println("Error updating temp file for sale: " + e.getMessage());
    }
  }

  public double endPOS(String textFile) {
    detectSystem();
    boolean bool = true;
    if (transactionItem.size() > 0) {
      totalPrice = totalPrice * tax; // calculates price with tax
      // prints total with taxes
      // bool=payment();

      // System.out.format("Total with taxes: %.2f\n", totalPrice);
      inventory.updateInventory(textFile, transactionItem, databaseItem, true);
    }
    // delete log file
    File file = new File(tempFile);
    file.delete();
    if (bool == true) {
      // invoice record file
      try {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        Calendar cal = Calendar.getInstance();
        String t = "../Database/saleInvoiceRecord.txt";
        if (System.getProperty("os.name").startsWith("W") || System.getProperty("os.name").startsWith("w")) {
          // t = "..\\Database\\saleInvoiceRecord.txt";
        }
        FileWriter fw2 = new FileWriter(t, true);
        BufferedWriter bw2 = new BufferedWriter(fw2);
        bw2.write(dateFormat.format(cal.getTime()));
        bw2.write(System.getProperty("line.separator"));
        for (int i = 0; i < transactionItem.size(); i++) {
          String log = Integer.toString(transactionItem.get(i).getItemID()) + " " + transactionItem.get(i).getItemName()
              + " " +
              Integer.toString(transactionItem.get(i).getAmount()) + " " +
              Double.toString(transactionItem.get(i).getPrice() * transactionItem.get(i).getAmount());
          bw2.write(log);
          bw2.write(System.getProperty("line.separator"));
        }
        bw2.write("Total with tax: " + totalPrice);
        bw2.newLine();
        bw2.close();

      } catch (FileNotFoundException e) {
        System.out.println("Unable to open file Log File for logout");
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    databaseItem.clear();
    transactionItem.clear();
    return totalPrice;
  }

  public void retrieveTemp(String textFile) {
    try {
      FileReader fileR = new FileReader(tempFile);
      BufferedReader textReader = new BufferedReader(fileR);
      String line = null;

      String[] lineSort;
      line = textReader.readLine();
      inventory.accessInventory(textFile, databaseItem);

      while ((line = textReader.readLine()) != null) {
        lineSort = line.split(" ");
        int itemNo = Integer.parseInt(lineSort[0]);
        int itemAmount = Integer.parseInt(lineSort[1]);
        enterItem(itemNo, itemAmount);
      }
      textReader.close();
      updateTotal();
    } catch (FileNotFoundException ex) {
      System.out.println(
          "Unable to open file 'temp'");
    } catch (IOException ex) {
      System.out.println(
          "Error reading file 'temp'");
    }

  }

}
