package hansen;
/*
Authors: James DeSelms, Josh Hansen, Sean Wilson
This program does the following:
 - 
 - 
 - 
*/
// *************************************** CODE *********************************************************
//To help with GUI.
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

import javax.swing.JLabel;
import javax.swing.JOptionPane;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.*;

//for card, hand, deck classes
import java.util.Arrays;
import java.util.Random;

//******************************* MAIN CLASS ********************************
public class LowCardGameClient
{  
   public static void main(String[] args) 
   {
      Model.startLowCardGame(1, 4, 0, null, 2, 7);
   }
}
//******************************* END MAIN CLASS *****************************


//**************************** THREADEDTIMER CLASS ***************************
//timer stuff
class ThreadedTimer extends Thread
{
   public Timer threadTimer = null;
   static Boolean timerRunning = false;
   static Boolean alreadySetup = false;
   static long seconds = 0;
   static long minutes = 0;
   static long start;

   public void run()
   {
      if (!timerRunning)
      {
         start = System.currentTimeMillis();
         threadTimer = new Timer(100, Controller.myTimerLabeler);
         threadTimer.start();
      }
   }
   
   void stopTimer()
   {
      threadTimer.stop();
      seconds = -1;
      minutes = 0;
   }
   
   static void doNothing(long duration) throws InterruptedException
   {
      sleep(duration);
   }
}
//************************* END THREADEDTIMER CLASS ************************


//***************************** MODEL CLASS ********************************
class Model
{
   //overall game constants and values
   static final int DEFAULT_NUM_PLAYERS = 2;
   static final int MIN_NUM_PLAYERS = 2;
   static final int MAX_NUM_PLAYERS = 2;
   static final int DEFAULT_NUM_CARDS = 7;
   static final int MIN_NUM_CARDS = 3;
   static final int MAX_NUM_CARDS = 11;
   private static int numPlayers;
   private static int numCardsPerHand;

   //values to keep track of plays and winnings
   static int whoWon = 0; //1 for CPU, 0 for human - human begins match
   static Card cpuPlayedCard;
   static Card userPlayedCard;
   static int cpuScore = 0;
   static int userScore = 0;
   static char cpuValue;
   static char userValue;
   static int handOffset = 0;
   static CardGameFramework LowCardGame;

   public static void startLowCardGame(int numPacksPerDeck, int numJokersPerPack,
         int numUnusedCardsPerPack, Card[] unusedCardsPerPack, int numPlayersParam, int numCardsPerHandsParam)
   {       
      setNumCardsPerHand(numCardsPerHandsParam);
      setNumPlayers(numPlayersParam);
      String[] introStringsParam = {"Play the lower card to win!\nAces are lowest and Jokers are highest.", "Low Card Game"};

      //Set the framework and cardtable
      LowCardGame = new CardGameFramework(numPacksPerDeck, numJokersPerPack,
            numUnusedCardsPerPack, unusedCardsPerPack, numPlayers, numCardsPerHand);

      //Deal and show the user.
      LowCardGame.deal();
      LowCardGame.sortHands();

      View.lowCardView(numCardsPerHand, numPlayers, introStringsParam);
   }

   private static Boolean setNumCardsPerHand(int numCardsPerHandsParam)
   {
      if ((numCardsPerHandsParam > MIN_NUM_CARDS) && (numCardsPerHandsParam < MAX_NUM_CARDS))
      {
         numCardsPerHand = numCardsPerHandsParam;
         return true;
      }
      else {
         numCardsPerHand = DEFAULT_NUM_CARDS;
         return false;
      }
   }

   private static Boolean setNumPlayers(int numPlayersParam)
   {
      if ((numPlayersParam < MIN_NUM_PLAYERS) && (numPlayersParam > MAX_NUM_PLAYERS))
      {
         numPlayers = numPlayersParam;
         return true;
      }
      else {
         numPlayers = DEFAULT_NUM_PLAYERS;
         return false;
      }
   }

   //A method to inspect the card.
   static Icon inspectCard(Card theCard)
   {

      //Return the icon.
      return GUICard.getIcon(theCard);

   }

   //A method that contains most of the game logic for Low Card game
   static void beginMatch(int cardIndex)
   {

      //If whoWon is equal to 0.
      if (whoWon == 0)
         //Remove everything from the pnlPlayArea.
         View.myCardTable.pnlPlayArea.removeAll();

      View.refreshPlayArea();

      //Human plays card and this is added to the play area
      userPlayedCard = LowCardGame.playCard(0, cardIndex);
      userValue = userPlayedCard.getValue();
      LowCardGame.sortHands();
      View.humanViewPlay();

      //CPU plays here if human won last round or if this is the first play, and this is added to the play area
      if (whoWon == 0)
      {
         cpuNextPlay(); //sets cpuPlayedCard
         View.cpuViewPlay();
      }

      //The user and CPU draw cards if there are cards left.
      if (LowCardGame.getNumCardsRemainingInDeck() >= 2)
      {
         LowCardGame.takeCard(0);
         LowCardGame.takeCard(1);
      }
      else
         handOffset++;

      //The hands are sorted.
      LowCardGame.sortHands();

      View.recreateHumanPanel();
      View.recreateCPUPanel();

      decideWinner();

      View.myCardTable.pnlPlayArea.removeAll(); //resetting the play area here for the next round to begin...

      //The CPU plays first if human lost last round, and this is added to the play area
      if (whoWon == 1)
      {
         cpuNextPlay(); //sets cpuPlayedCard
         View.cpuViewPlay();
      }

   }
   
   static void decideWinner()
   {
      //Decide on the winner and update scores
      if (((GUICard.turnCardValueIntoInt(userValue) < GUICard.turnCardValueIntoInt(cpuValue)))
            || ((GUICard.turnCardValueIntoInt(userValue) == GUICard.turnCardValueIntoInt(cpuValue))
                  && (GUICard.turnCardSuitIntoInt(userPlayedCard.getSuit()) < GUICard.turnCardSuitIntoInt(cpuPlayedCard.getSuit()))))
      {
         whoWon = 0;
         userScore++;
      }
      else
      {
         whoWon = 1;
         cpuScore++;
      }

      if ((handOffset == numCardsPerHand)) // if cards are out, show the final winnings in a dialog and exit
      {
         if (userScore > cpuScore)
            whoWon = 0;
         else if (userScore < cpuScore)
            whoWon = 1;
         else
            whoWon = 2;

         View.endGameDialog();

         System.exit(0);

      } 
      else //show the round winnings in a dialog and continue
         View.nextRoundDialog();
   }

   //the logic for the CPU plays
   //Method that governs the CPU.
   static void cpuNextPlay()
   {
      int k;
      Boolean lowerCardFound = false;
      int handSize = LowCardGame.getHand(1).getNumCards();

      LowCardGame.sortHands();

      // if CPU goes first, semi-randomly choosing next card from scratch
      if (whoWon == 1)
      {
         Random rand = new Random();
         int randInt = rand.nextInt(100);


         //randomized but weighted index for which of lowest 4 to pick
         //then use that index to get the computer card, removing from hand
         //checking along the way if we have a hand too small for the card level chosen
         if (randInt > 60)
            cpuPlayedCard = LowCardGame.playCard(1, 0);
         else if (randInt > 30)
         {
            if (handOffset < numCardsPerHand - 2)
               cpuPlayedCard = LowCardGame.playCard(1, 1);
            else
               cpuPlayedCard = LowCardGame.playCard(1, 0);
         }
         else
         {
            if (handOffset < numCardsPerHand - 3)
               cpuPlayedCard = LowCardGame.playCard(1, 2);
            else if (handOffset < numCardsPerHand - 2)
               cpuPlayedCard = LowCardGame.playCard(1, 1);
            else
               cpuPlayedCard = LowCardGame.playCard(1, 0);
         }
      }

      //if human went first, CPU will choose highest winning card or the highest card in the deck as a throw-away
      if (whoWon == 0)
      {
         //sort computer hand
         //starting from biggest, 
         for (k = handSize - 1 - handOffset; k >= 0; k--)
         {
            //loop through computer hand from greatest to least until smaller card is found
            if ((((GUICard.turnCardValueIntoInt(LowCardGame.getHand(1).inspectCard(k).getValue()))
                  < GUICard.turnCardValueIntoInt(userPlayedCard.getValue()))) 
                  || (((GUICard.turnCardValueIntoInt(LowCardGame.getHand(1).inspectCard(k).getValue()))
                        == GUICard.turnCardValueIntoInt(userPlayedCard.getValue()))
                        && (GUICard.turnCardSuitIntoInt(LowCardGame.getHand(1).inspectCard(k).getSuit())
                              < GUICard.turnCardSuitIntoInt(userPlayedCard.getSuit()))))
            {
               lowerCardFound = true;
               cpuPlayedCard = LowCardGame.playCard(1, k); //play the low card found
               break;
            }

         }

         //if none found, send biggest card instead
         //removing that card from the hand at the same time
         if (!lowerCardFound)
         {
            cpuPlayedCard = LowCardGame.playCard(1, numCardsPerHand - 1 - handOffset);
         }
      }

      cpuValue = cpuPlayedCard.getValue();
      LowCardGame.sortHands();
   }
}
//******************************** END MODEL CLASS ************************


//***************************** VIEW CLASS ********************************
class View
{
   //overall game parameters
   private static int numPlayers;
   private static int numCardsPerHand;
   private static String[] gameIntroStrings;

   //GUI framework to output the game to the user
   static CardTable myCardTable;
   static JLabel[] computerLabels;
   static JLabel[] humanLabels;
   static JButton[] humanButton;
   static JLabel[] playedCardLabels;
   static JLabel[] playLabelText;
   static Icon tempIcon;
   static String[] playLabelStrings = {"You", "Computer"};
   static String[] winnerStrings = {"You Win!", "CPU won.", "It's a tie."};
   
   //timer stuff
   //Initialize and setup timer panel and buttons
   static JPanel timerPanel = new JPanel();
   static JLabel timerLabel = new JLabel("0:00");
   static JButton startStopButton;
   static void setupTimerPanel()
   {
      //Create and add the timer panel and label
      myCardTable.add(timerPanel, BorderLayout.EAST);
      timerPanel.setLayout(new BorderLayout());
      timerPanel.add(timerLabel, BorderLayout.PAGE_START);
      timerPanel.setBorder(BorderFactory.createTitledBorder("Timer"));
      timerLabel.setFont (timerLabel.getFont ().deriveFont (64.0f));
      
      //create and add the button
      startStopButton = new JButton("Start/Stop");
      startStopButton.setActionCommand("Start/Stop");
      startStopButton.addActionListener(Controller.myTimerListener);
      timerPanel.add(startStopButton, BorderLayout.PAGE_END);
   }
   
   // Establishing the GUI for Low Card Game
   static void lowCardView(int numCardsPerHandsParam, int numPlayersParam, String[] introStringsParam)
   {
      numPlayers = numPlayersParam;
      numCardsPerHand = numCardsPerHandsParam;
      gameIntroStrings = introStringsParam;

      computerLabels = new JLabel[numCardsPerHand];
      humanLabels = new JLabel[numCardsPerHand];  
      humanButton = new JButton[humanLabels.length];
      playedCardLabels  = new JLabel[numPlayers]; 
      playLabelText  = new JLabel[numPlayers];

      myCardTable = new CardTable("LowCardTable", numCardsPerHand, numPlayers);

      //Set up the GUI.
      myCardTable.setSize(800, 500);
      myCardTable.setLocationRelativeTo(null);
      myCardTable.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      
      //Initialize the timer
      setupTimerPanel();

      //Make all of the buttons for the user and add them
      for (int i = 0; i < humanLabels.length - Model.handOffset; i++) {

         tempIcon = new ImageIcon(Model.inspectCard(Model.LowCardGame.getHand(0).inspectCard(i)).toString());
         humanLabels[i] = new JLabel(tempIcon);
         humanButton[i] = new JButton(tempIcon);
         humanButton[i].setActionCommand("Play" + i);
         humanButton[i].addActionListener(Controller.bp);
         humanButton[i].setBorderPainted(false);
         myCardTable.pnlHumanHand.add(humanButton[i]);
      }

      //Initialize all of the labels for the CPU
      for (int i = 0; i < computerLabels.length - Model.handOffset; i++) {

         tempIcon = new ImageIcon(GUICard.getCardBackIcon().toString());
         computerLabels[i] = new JLabel(tempIcon);
      }

      recreateCPUPanel();
      recreateHumanPanel();

      //Show the user.
      myCardTable.setVisible(true);

      JOptionPane.showMessageDialog(myCardTable.pnlPlayArea, gameIntroStrings[0], gameIntroStrings[1], 1);
   }
   //Replace computer cardbacks
   static void recreateCPUPanel()
   {
      myCardTable.pnlComputerHand.removeAll();
      for (int i = 0; i < computerLabels.length - Model.handOffset; i++)
         myCardTable.pnlComputerHand.add(computerLabels[i]);
      myCardTable.pnlComputerHand.updateUI();
      myCardTable.pnlComputerHand.repaint();
   }

   //Remove everything from the user's side, create it again, and put in into place.
   static void recreateHumanPanel()
   {
      myCardTable.pnlHumanHand.removeAll();
      for (int i = 0; i < humanLabels.length - Model.handOffset; i++) {

         tempIcon = new ImageIcon(Model.inspectCard(Model.LowCardGame.getHand(0).inspectCard(i)).toString());
         humanLabels[i] = new JLabel(tempIcon);
         humanButton[i] = new JButton(tempIcon);
         humanButton[i].setActionCommand("Play" + i);
         humanButton[i].addActionListener(Controller.bp);
         humanButton[i].setBorderPainted(false);
         myCardTable.pnlHumanHand.add(humanButton[i]);
      }

   }

   static void refreshPlayArea()
   {
      myCardTable.pnlPlayArea.updateUI();
      myCardTable.setVisible(true);
      myCardTable.pnlPlayArea.repaint();
   }

   static void cpuViewPlay()
   {
      playedCardLabels[1] = new JLabel(new ImageIcon((Model.inspectCard(Model.cpuPlayedCard).toString())));
      playedCardLabels[1].setBorder(BorderFactory.createTitledBorder(playLabelStrings[1]));
      myCardTable.pnlPlayArea.add(playedCardLabels[1]);
      refreshPlayArea();
   }

   static void humanViewPlay()
   {
      playedCardLabels[0] = new JLabel(new ImageIcon(Model.inspectCard(Model.userPlayedCard).toString()));
      playedCardLabels[0].setBorder(BorderFactory.createTitledBorder(playLabelStrings[0]));
      myCardTable.pnlPlayArea.add(playedCardLabels[0]);
      myCardTable.setVisible(true);
      myCardTable.pnlPlayArea.repaint();
   }

   static void nextRoundDialog()
   {
      JOptionPane.showMessageDialog(View.myCardTable.pnlHumanHand, ("Your Score: " + Model.userScore + "\nCPU Score: "
            + Model.cpuScore + "\n\nPress OK for next round."), View.winnerStrings[Model.whoWon], 1);
   }

   static void endGameDialog()   
   {
      JOptionPane.showMessageDialog(View.myCardTable.pnlHumanHand, ("FINAL SCORES:\n" + "\nYou: " + Model.userScore + "\nCPU: "
            + Model.cpuScore + "\n\nPress OK to end game."), View.winnerStrings[Model.whoWon], 1);
   }
}
//***************************** END VIEW CLASS *****************************


//************************* CONTROLLER CLASS *******************************
class Controller
{
   static buttonPressed bp = new buttonPressed();
   
   //timer stuff
   static TimerListener myTimerListener = new TimerListener();
   static TimerLabeler myTimerLabeler = new TimerLabeler();
   static ThreadedTimer myThreadedTimer = new ThreadedTimer();
   //timer stuff
   static class TimerListener implements ActionListener //Start or stop timer
   {     
      public void actionPerformed(ActionEvent e)
      {
         if (ThreadedTimer.timerRunning)
         {
            myThreadedTimer.stopTimer();
            ThreadedTimer.timerRunning = false;
         }
         else
         {
            myThreadedTimer.run();
            ThreadedTimer.timerRunning = true;
         }
      }
   }
   static class TimerLabeler implements ActionListener //Updates the timer text
   {     
      public void actionPerformed(ActionEvent e)
      {                        
         String secondsString = "";
         long currentMill = System.currentTimeMillis(); //for current-start time that doesn't use doNothing
         ThreadedTimer.minutes = ((currentMill - ThreadedTimer.start) / 1000) / 60;
         ThreadedTimer.seconds = ((currentMill - ThreadedTimer.start) / 1000) % 60;
         
         
         if (ThreadedTimer.seconds < 10)
            secondsString = "0" + Long.toString(ThreadedTimer.seconds);
         else
            secondsString = Long.toString(ThreadedTimer.seconds);
         
         View.timerLabel.setText(Long.toString(ThreadedTimer.minutes) + ":" + secondsString);
         
         try
         {
            myThreadedTimer.doNothing(1000);
         } catch (InterruptedException e1)
         {
            // TODO Auto-generated catch block
            System.out.println("Failed to pause.");
         }
      }
   }

   //If the button was pressed, intilializes the next round...
   static class buttonPressed implements ActionListener {

      public void actionPerformed(ActionEvent e) {

         //Depending on the button, call beginMatch with an int argument, where the int is the button pushed.
         switch (e.getActionCommand()) {

         case "Play0":
            Model.beginMatch(0);
            break;
         case "Play1":
            Model.beginMatch(1);
            break;
         case "Play2":
            Model.beginMatch(2);
            break;
         case "Play3":
            Model.beginMatch(3);
            break;
         case "Play4":
            Model.beginMatch(4);
            break;
         case "Play5":
            Model.beginMatch(5);
            break;
         case "Play6":
         default:
            Model.beginMatch(6);
            break;

         }
      }
   }
   

}
//************************ END CONTROLLER CLASS ****************************