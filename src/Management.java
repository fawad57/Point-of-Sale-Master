
import java.io.*;
import java.util.*;
import java.text.*;

public class Management {

  private static String userDatabase = "../Database/userDatabase.txt";

  public Management() {

    if (System.getProperty("os.name").startsWith("W") || System.getProperty("os.name").startsWith("w")) {
      // userDatabase="..\\Database\\userDatabase.txt";
    } else {
      userDatabase = "../Database/userDatabase.txt";
    }

  }

  public Boolean checkUser(Long phone) { // returns true if user phone is in DB, false if not
    // needs to be cleaned up.. written with terrible style right now but *at least
    // it works*
    // check user will open the user database and check to see if user's phone
    // number is on the list
    try {
      FileReader fileR = new FileReader(userDatabase);
      BufferedReader textReader = new BufferedReader(fileR);
      String line;
      long nextPh = 0;
      // reads the entire database
      line = textReader.readLine(); // skips the first line, which explains how the DB is formatted.
      while ((line = textReader.readLine()) != null) {

        try {
          nextPh = Long.parseLong(line.split(" ")[0]);
        } catch (NumberFormatException e) {
          continue;
        }

        if (nextPh == phone) {
          textReader.close();
          fileR.close();
          // System.out.println("user phone number found in userDatabase");
          return true;
        }
        // System.out.println(line.split(" ")[0]);
      }
      textReader.close();
      fileR.close();
      // System.out.println("reached end of userDatabase, phone number not found");
      return false;
    }
    // catches exceptions
    catch (FileNotFoundException ex) {
      System.out.println("cannot open userDB");

    } catch (IOException ex) {
      System.out.println("ioexception");
    }
    return true;
  }

  public List<ReturnItem> getLatestReturnDate(Long phone) {
    SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yy");
    List<String> userLines = readUserDatabaseLines();
    String userLine = findUserLineForPhone(userLines, phone);

    if (userLine == null) {
      return Collections.emptyList();
    }

    List<ReturnItem> outstandingItems = parseOutstandingRentalItems(userLine, formatter);
    if (outstandingItems.isEmpty()) {
      System.out.println("No outstanding returns");
    }
    return outstandingItems;
  }

  private List<String> readUserDatabaseLines() {
    List<String> lines = new ArrayList<>();
    try (BufferedReader reader = new BufferedReader(new FileReader(userDatabase))) {
      // Skip header
      String line = reader.readLine();
      while ((line = reader.readLine()) != null) {
        lines.add(line);
      }
    } catch (FileNotFoundException ex) {
      System.out.println("cannot open userDB");
    } catch (IOException ex) {
      System.out.println("ioexception");
    }
    return lines;
  }

  private String findUserLineForPhone(List<String> lines, long phone) {
    for (String line : lines) {
      String[] tokens = line.split(" ");
      if (tokens.length == 0) {
        continue;
      }
      try {
        long nextPh = Long.parseLong(tokens[0]);
        if (nextPh == phone) {
          return line;
        }
      } catch (NumberFormatException ignored) {
        // Skip malformed lines
      }
    }
    return null;
  }

  private List<ReturnItem> parseOutstandingRentalItems(String userLine, SimpleDateFormat formatter) {
    List<ReturnItem> result = new ArrayList<>();
    String[] tokens = userLine.split(" ");
    if (tokens.length <= 1) {
      return result; // no rentals
    }

    for (int i = 1; i < tokens.length; i++) {
      String[] rentalParts = tokens[i].split(",");
      if (rentalParts.length != 3) {
        continue;
      }

      String itemIdStr = rentalParts[0];
      String dueDateStr = rentalParts[1];
      String returnedStr = rentalParts[2];

      if (Boolean.parseBoolean(returnedStr)) {
        continue; // already returned
      }

      try {
        int itemId = Integer.parseInt(itemIdStr);
        Date dueDate = formatter.parse(dueDateStr);

        Calendar dueCal = Calendar.getInstance();
        dueCal.setTime(dueDate);
        int daysLate = daysBetween(dueCal);

        result.add(new ReturnItem(itemId, daysLate));
      } catch (NumberFormatException | ParseException e) {
        e.printStackTrace();
      }
    }
    return result;
  }

  private static int daysBetween(Calendar day1) {

    Calendar day2 = Calendar.getInstance();

    Calendar dayOne = (Calendar) day1.clone(),
        dayTwo = (Calendar) day2.clone();

    if (dayOne.get(Calendar.YEAR) == dayTwo.get(Calendar.YEAR)) {
      return (dayTwo.get(Calendar.DAY_OF_YEAR) - dayOne.get(Calendar.DAY_OF_YEAR));
    } else {
      if (dayTwo.get(Calendar.YEAR) > dayOne.get(Calendar.YEAR)) {
        // swap them
        Calendar temp = dayOne;
        dayOne = dayTwo;
        dayTwo = temp;
      }
      int extraDays = 0;

      int dayOneOriginalYearDays = dayOne.get(Calendar.DAY_OF_YEAR);

      while (dayOne.get(Calendar.YEAR) > dayTwo.get(Calendar.YEAR)) {
        dayOne.add(Calendar.YEAR, -1);
        // getActualMaximum() important for leap years
        extraDays += dayOne.getActualMaximum(Calendar.DAY_OF_YEAR);
      }

      return extraDays - dayTwo.get(Calendar.DAY_OF_YEAR) + dayOneOriginalYearDays;
    }
  }

  public boolean createUser(Long phone) {

    String strPhone = Long.toString(phone);
    File file = new File(userDatabase);
    try {
      PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(file, true)));
      out.println();
      out.print(strPhone);
      out.close();
      return true;
    } catch (IOException e) {
      System.out.println("cannot write to userDB");
      return false;
    }
  }

  public static void addRental(long phone, List<Item> rentalList) {
    long nextPhone = 0;
    List<String> fileList = new ArrayList<String>();
    Date date = new Date();
    Format formatter = new SimpleDateFormat("MM/dd/yy");
    String dateFormat = formatter.format(date);
    boolean ableToRead = false;

    // Reads from file to read the changes to make:
    try {
      ableToRead = true;
      FileReader fileR = new FileReader(userDatabase);
      BufferedReader textReader = new BufferedReader(fileR);
      String line;
      // reads the entire database
      line = textReader.readLine(); // skips the first line, which explains how the DB is formatted.
      fileList.add(line); // but stores it since it is the first line of the DB
      while ((line = textReader.readLine()) != null) {

        try {
          nextPhone = Long.parseLong(line.split(" ")[0]);
        } catch (NumberFormatException e) {
          continue;
        }
        System.out.println("comparing " + nextPhone + " == " + phone);
        if (nextPhone == phone)// finds the user in the database
        {

          // loop through each "ID" in rentalList
          for (Item item : rentalList) {
            line = line + " " + item.getItemID() + "," + dateFormat + "," + "false";
          }

          fileList.add(line);
        } else
          fileList.add(line); // adds the lines that are not modified from the database to the list to be
                              // rewritten later

      }
      textReader.close();
      fileR.close();
    }

    // catches exceptions
    catch (FileNotFoundException ex) {
      System.out.println("cannot open userDB");

    } catch (IOException ex) {
      System.out.println("ioexception");
    }

    // Now writes to file to make the changes:
    if (ableToRead) // if file has been read throughly
    {
      try {
        File file = new File(userDatabase);
        FileWriter fileR = new FileWriter(file.getAbsoluteFile());
        BufferedWriter bWriter = new BufferedWriter(fileR);
        PrintWriter writer = new PrintWriter(bWriter);

        for (int wCounter = 0; wCounter < fileList.size(); ++wCounter)
          writer.println(fileList.get(wCounter));

        bWriter.close(); // closes writer
      }

      catch (IOException e) {
      }
      {
      }
    }

  }

  public void updateRentalStatus(long phone, List<ReturnItem> returnedList) {
    long nextPhone = 0;
    List<String> fileList = new ArrayList<String>();
    String modifiedLine;
    Date date = new Date();
    Format formatter = new SimpleDateFormat("MM/dd/yy");
    String dateFormat = formatter.format(date);
    boolean ableToRead = false;

    // Reads from file to read the changes to make:

    try {
      ableToRead = true;
      FileReader fileR = new FileReader(userDatabase);
      BufferedReader textReader = new BufferedReader(fileR);
      String line;
      int returnCounter = 0;
      // reads the entire database
      line = textReader.readLine(); // skips the first line, which explains how the DB is formatted.
      fileList.add(line); // but stores it since it is the first line of the DB
      while ((line = textReader.readLine()) != null) {

        try {
          nextPhone = Long.parseLong(line.split(" ")[0]);
        } catch (NumberFormatException e) {
          continue;
        }
        if (nextPhone == phone)// finds the user in the database
        {
          modifiedLine = line.split(" ")[0];
          if (line.split(" ").length > 1) {

            for (int i = 1; i < line.split(" ").length; i++) {
              String returnedBool = (line.split(" ")[i]).split(",")[2];

              boolean b = returnedBool.equalsIgnoreCase("true");
              if (!b)// if item wasn't returned already
              {
                for (returnCounter = 0; returnCounter < returnedList.size(); returnCounter++)
                  if (Integer.parseInt(line.split(" ")[i].split(",")[0]) == returnedList.get(returnCounter)
                      .getItemID()) {
                    modifiedLine += " " + line.split(" ")[i].split(",")[0] + "," + dateFormat + "," + "true";

                  }
                /*
                 * if (returnCounter == returnedList.size() )
                 * modifiedLine += line.split(" ")[i]; //not returning this item now
                 */
              }

              else {
                modifiedLine += " " + line.split(" ")[i];
              }
            }
          }
          fileList.add(modifiedLine);
        } else
          fileList.add(line); // adds the lines that are not modified from the database to the list to be
                              // rewritten later

      }
      textReader.close();
      fileR.close();
    }

    // catches exceptions
    catch (FileNotFoundException ex) {
      System.out.println("cannot open userDB");

    } catch (IOException ex) {
      System.out.println("ioexception");
    }

    // Now writes to file to make the changes:
    if (ableToRead) // if file has been read throughly
    {
      try {
        File file = new File(userDatabase);
        FileWriter fileR = new FileWriter(file.getAbsoluteFile());
        BufferedWriter bWriter = new BufferedWriter(fileR);
        PrintWriter writer = new PrintWriter(bWriter);

        for (int wCounter = 0; wCounter < fileList.size(); ++wCounter)
          writer.println(fileList.get(wCounter));

        bWriter.close(); // closes writer
      }

      catch (IOException e) {
      }
      {
      }
    }

  }

}
