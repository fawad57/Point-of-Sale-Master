import java.awt.Color;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class Payment_Interface extends JFrame implements ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JButton PayCash;
	private JButton PayElectronic;
	private JButton cancelTransaction;
	private JButton confirm;
	private long phoneNum;
	private JTextArea transactionDialog;

	JScrollPane scroll;

	PointOfSale transaction;
	String database;
	String operation;

	boolean returnOrNot;

	public Payment_Interface(PointOfSale transaction, String databaseFile, String operation, String phone, boolean r)

	{
		super("SG Technologies - Payment View");
		setLayout(null);

		returnOrNot = true;

		this.transaction = transaction;
		this.database = databaseFile;
		returnOrNot = r;

		Toolkit tk = Toolkit.getDefaultToolkit();
		int xSize = ((int) tk.getScreenSize().getWidth());
		int ySize = ((int) tk.getScreenSize().getHeight());

		setSize(xSize, ySize);

		this.operation = operation;

		if (operation.equals("Return"))
			phoneNum = Long.parseLong(phone);

		PayCash = new JButton("Cash Payment");
		PayCash.setBounds(xSize * 4 / 5, ySize / 4, 150, 80);
		add(PayCash);

		PayElectronic = new JButton("Pay Electronically");
		PayElectronic.setBounds(xSize * 4 / 5, ySize * 2 / 4, 150, 80);
		add(PayElectronic);

		confirm = new JButton("Confirm Payment");
		confirm.setBounds(xSize * 4 / 5, ySize / 4, 150, 80);

		cancelTransaction = new JButton("Cancel");
		cancelTransaction.setBounds(xSize * 4 / 5, ySize * 3 / 4, 150, 80);
		add(cancelTransaction);

		transactionDialog = new JTextArea();
		transactionDialog.setBackground(Color.white);
		transactionDialog.setForeground(Color.black);
		transactionDialog.setEditable(false);
		Font font = transactionDialog.getFont();
		float size = font.getSize() + 5.0f;
		transactionDialog.setFont(font.deriveFont(size));

		scroll = new JScrollPane(transactionDialog,
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setBounds(xSize / 16, ySize / 16, 3 * xSize / 5, 4 * ySize / 5);
		add(scroll);

		updateText();

		PayCash.addActionListener(this);
		PayElectronic.addActionListener(this);
		cancelTransaction.addActionListener(this);
		confirm.addActionListener(this);

	}

	@Override
	public void actionPerformed(ActionEvent event) {
		Object source = event.getSource();
		if (source == PayCash) {
			handleCashPayment();
		} else if (source == PayElectronic) {
			handleElectronicPayment();
		} else if (source == cancelTransaction) {
			handleCancel();
		} else if (source == confirm) {
			handleConfirm();
		}
	}

	private void handleCashPayment() {
		double cash = readCashAmount();
		while (cash < transaction.getTotal()) {
			JOptionPane.showMessageDialog(this, "Value must be greater than total value");
			cash = readCashAmount();
		}

		double change = cash - transaction.getTotal();
		if (change > 0) {
			JOptionPane.showMessageDialog(this, "Change $:" + String.format("%.2f", change));
		}

		transactionDialog.append("\n\nPaid: $" + String.format("%.2f", cash) + "\n");
		transactionDialog.append("Change: $" + String.format("%.2f", change) + "\n");

		if ("Rental".equals(operation)) {
			appendReturnDate();
		}

		switchToConfirmationState();
	}

	private double readCashAmount() {
		String input = JOptionPane.showInputDialog(this, "Amount payed on cash:");
		return Double.parseDouble(input);
	}

	private void handleElectronicPayment() {
		String cardNo = JOptionPane.showInputDialog(this, "Card Number:");
		if (!transaction.creditCard(cardNo)) {
			JOptionPane.showMessageDialog(this, "Invalid credit card number");
			return;
		}

		if (!returnOrNot) {
			String cashBackString = JOptionPane.showInputDialog(this, "If you desire cash back, type the quantity");
			double cashBack = (cashBackString == null || cashBackString.isEmpty())
					? 0.0
					: Double.parseDouble(cashBackString);

			transactionDialog.append("\n\nCash back: $" + String.format("%.2f", cashBack) + "\n");
			transactionDialog.append("Total price: $" +
					String.format("%.2f", cashBack + transaction.getTotal()) + "\n");

			if ("Rental".equals(operation)) {
				appendReturnDate();
			}
		}

		switchToConfirmationState();
	}

	private void switchToConfirmationState() {
		remove(PayCash);
		remove(PayElectronic);
		remove(cancelTransaction);
		add(confirm);
		revalidate();
		repaint();
	}

	private void handleCancel() {
		JOptionPane.showMessageDialog(this, "Transaction canceled");
		navigateBackToCashier();
	}

	private void handleConfirm() {
		JOptionPane.showMessageDialog(this, "Payment confirmed");
		navigateBackToCashier();
	}

	private void navigateBackToCashier() {
		POSSystem sys = new POSSystem();
		Cashier_Interface cashier = new Cashier_Interface(sys);
		cashier.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		cashier.setVisible(true);

		setVisible(false);
		dispose();
	}

	private void updateText() {
		if (operation.equals("Return")) {
			List<ReturnItem> returnList;
			List<Item> transactionItem;

			Management management = new Management();
			returnList = management.getLatestReturnDate(phoneNum);
			transactionItem = transaction.getCart();

			double itemPrice = 0;
			transactionDialog.setText(null);
			for (int transactionCounter = 0; transactionCounter < transactionItem.size(); transactionCounter++)
				for (int returnCounter = 0; returnCounter < returnList.size(); returnCounter++) {
					if (transactionItem.get(transactionCounter).getItemID() == returnList.get(returnCounter)
							.getItemID()) {
						// Applies a value to be payed depending on the amount of days it is late. If it
						// is not late, no value is applied
						itemPrice = transactionItem.get(transactionCounter).getAmount()
								* transactionItem.get(transactionCounter).getPrice() * 0.1
								* returnList.get(returnCounter).getDays();
						transactionDialog.append(
								"Item ID: " + transactionItem.get(transactionCounter).getItemID() + "    Item Name: "
										+ transactionItem.get(transactionCounter).getItemName() + "    Amount: x"
										+ transactionItem.get(transactionCounter).getAmount() +
										"    Days Late: " + returnList.get(returnCounter).getDays() + "   To be paid: $"
										+ itemPrice + "\n");

					}
				}
			transactionDialog.append("\nTotal: $" + String.format("%.2f", transaction.endPOS(database)) + "\n");
		} else {
			transactionDialog.setText(null);
			List<Item> transactionItem = transaction.getCart();
			for (Item temp : transactionItem) {
				String itemString = temp.getItemID() + "\t" + temp.getItemName() + " \t" + "x" + temp.getAmount()
						+ "\t$" + String.format("%.2f", temp.getAmount() * temp.getPrice()) + "\n";
				transactionDialog.append(itemString);
			}
			transactionDialog.append("\nTotal: $" + String.format("%.2f", transaction.getTotal()) + "\n");

			double totalWithTax = transaction.endPOS(database);
			transactionDialog.append("Total with taxes: $" + String.format("%.2f", totalWithTax) + "\n");
		}
	}

	private void appendReturnDate() {
		DateFormat df = new SimpleDateFormat("MM/dd/yy");
		Calendar calobj = Calendar.getInstance();
		calobj.add(Calendar.DATE, 14);
		transactionDialog.append("\nReturn Date: " + df.format(calobj.getTime()));
	}
}
