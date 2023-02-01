package hansen;

// *************************************** CODE *********************************************************
//To help with GUI.
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.util.Arrays;

//for Card, Hand, and Deck:
import java.util.Random; //Used to shuffle.

public class SupportingClasses
{

}

//************************** CARDGAMEFRAMEWORK CLASS ***************************
class CardGameFramework
{
   private static final int MAX_PLAYERS = 50;

   private int numPlayers;
   private int numPacks;            // # standard 52-card packs per deck
   // ignoring jokers or unused cards
   private int numJokersPerPack;    // if 2 per pack & 3 packs per deck, get 6
   private int numUnusedCardsPerPack;  // # cards removed from each pack
   private int numCardsPerHand;        // # cards to deal each player
   private Deck deck;               // holds the initial full deck and gets
   // smaller (usually) during play
   private Hand[] hand;             // one Hand for each player
   private Card[] unusedCardsPerPack;   // an array holding the cards not used
   // in the game.  e.g. pinochle does not
   // use cards 2-8 of any suit

   public CardGameFramework( int numPacks, int numJokersPerPack,
         int numUnusedCardsPerPack,  Card[] unusedCardsPerPack,
         int numPlayers, int numCardsPerHand)
   {
      int k;

      // filter bad values
      if (numPacks < 1 || numPacks > 6)
         numPacks = 1;
      if (numJokersPerPack < 0 || numJokersPerPack > 4)
         numJokersPerPack = 0;
      if (numUnusedCardsPerPack < 0 || numUnusedCardsPerPack > 50) //  > 1 card
         numUnusedCardsPerPack = 0;
      if (numPlayers < 1 || numPlayers > MAX_PLAYERS)
         numPlayers = 4;
      // one of many ways to assure at least one full deal to all players
      if  (numCardsPerHand < 1 ||
            numCardsPerHand >  numPacks * (52 - numUnusedCardsPerPack)
            / numPlayers )
         numCardsPerHand = numPacks * (52 - numUnusedCardsPerPack) / numPlayers;

      // allocate
      this.unusedCardsPerPack = new Card[numUnusedCardsPerPack];
      this.hand = new Hand[numPlayers];
      for (k = 0; k < numPlayers; k++)
         this.hand[k] = new Hand();
      deck = new Deck(numPacks);

      // assign to members
      this.numPacks = numPacks;
      this.numJokersPerPack = numJokersPerPack;
      this.numUnusedCardsPerPack = numUnusedCardsPerPack;
      this.numPlayers = numPlayers;
      this.numCardsPerHand = numCardsPerHand;
      for (k = 0; k < numUnusedCardsPerPack; k++)
         this.unusedCardsPerPack[k] = unusedCardsPerPack[k];

      // prepare deck and shuffle
      newGame();
   }

   // constructor overload/default for game like bridge
   public CardGameFramework()
   {
      this(1, 0, 0, null, 4, 13);
   }

   public Hand getHand(int k)
   {
      // hands start from 0 like arrays

      // on error return automatic empty hand
      if (k < 0 || k >= numPlayers)
         return new Hand();

      return hand[k];
   }

   public Card getCardFromDeck() { return deck.dealCard(); }

   public int getNumCardsRemainingInDeck() { return deck.getNumCards(); }

   public void newGame()
   {
      int k, j;

      // clear the hands
      for (k = 0; k < numPlayers; k++)
         hand[k].resetHand();

      // restock the deck
      deck.init(numPacks);

      // remove unused cards
      for (k = 0; k < numUnusedCardsPerPack; k++)
         deck.removeCard( unusedCardsPerPack[k] );

      // add jokers
      for (k = 0; k < numPacks; k++)
         for ( j = 0; j < numJokersPerPack; j++)
            deck.addCard( new Card('X', Card.Suit.values()[j]) );

      // shuffle the cards
      deck.shuffle();
   }

   public boolean deal()
   {
      // returns false if not enough cards, but deals what it can
      int k, j;
      boolean enoughCards;

      // clear all hands
      for (j = 0; j < numPlayers; j++)
         hand[j].resetHand();

      enoughCards = true;
      for (k = 0; k < numCardsPerHand && enoughCards ; k++)
      {
         for (j = 0; j < numPlayers; j++)
            if (deck.getNumCards() > 0)
            {
               hand[j].takeCard( deck.dealCard() );
            }
            else
            {
               enoughCards = false;
               break;
            }
      }

      return enoughCards;
   }

   void sortHands()
   {
      int k;

      for (k = 0; k < numPlayers; k++)
         hand[k].sort();
   }

   Card playCard(int playerIndex, int cardIndex)
   {
      // returns bad card if either argument is bad
      if (playerIndex < 0 ||  playerIndex > numPlayers - 1 ||
            cardIndex < 0 || cardIndex > numCardsPerHand - 1)
      {
         //Creates a card that does not work
         return new Card('M', Card.Suit.SPADES);      
      }

      // return the card played
      return hand[playerIndex].playCard(cardIndex);

   }


   boolean takeCard(int playerIndex)
   {
      // returns false if either argument is bad
      if (playerIndex < 0 || playerIndex > numPlayers - 1)
         return false;

      // Are there enough Cards?
      if (deck.getNumCards() <= 0)
         return false;

      return hand[playerIndex].takeCard(deck.dealCard());
   }

}
//*********************** END CARDGAMEFRAMEWORK CLASS ************************

//******************************* CARDTABLE CLASS ****************************
class CardTable extends JFrame
{
   //constant string for panels
   static final String[] BORDER_STRINGS = {"Computer Hand", "Play Area", 
   "Human Hand"};
   static int MAX_CARDS_PER_HAND = 56; //single deck size with jokers
   static int MAX_PLAYERS = 2;  // for now, we only allow 2 person games

   //hand size and number of hands
   private int numCardsPerHand;
   private int numPlayers;

   //setting the 3 panels of the game
   public JPanel pnlComputerHand, pnlHumanHand, pnlPlayArea;

   CardTable(String title, int numCardsPerHand, int numPlayers)
   {
      //validating input
      if (numCardsPerHand > MAX_CARDS_PER_HAND)
         numCardsPerHand = MAX_CARDS_PER_HAND;
      if (numPlayers > MAX_PLAYERS)
         numPlayers = MAX_PLAYERS;

      //setting panels up
      pnlComputerHand = new JPanel();
      pnlPlayArea = new JPanel();
      pnlHumanHand = new JPanel();

      //setting the layout
      setLayout(new BorderLayout());

      //adding titles
      pnlComputerHand.setBorder
      (BorderFactory.createTitledBorder(BORDER_STRINGS[0]));
      pnlPlayArea.setBorder
      (BorderFactory.createTitledBorder(BORDER_STRINGS[1]));
      pnlHumanHand.setBorder
      (BorderFactory.createTitledBorder(BORDER_STRINGS[2]));
      

      //adding the panels to the JFrame
      add(pnlComputerHand, BorderLayout.PAGE_START);
      add(pnlPlayArea, BorderLayout.CENTER);
      add(pnlHumanHand, BorderLayout.PAGE_END);
   }

   //accessor for hand size
   public int getnumCardsPerHand()
   {
      return numCardsPerHand;
   }

   //accessor for number of hands   
   public int getnumPlayers()
   {
      return numPlayers;
   }

}
//***************************** END CARDTABLE CLASS ****************************


//***************************** GUICARD CLASS **********************************
class GUICard {

   //Sine constants to avoid using literals.
   static final int NUM_SUITS = 4;
   static final int NUM_RANKS = 14;
   //A two-dimensional Icon array to hold the icons.
   private static Icon[][] iconCards = new ImageIcon[NUM_RANKS][NUM_SUITS];
   //An icon to hold the card back icon.
   private static Icon iconBack;
   //A boolean for whether the icons have been loaded.
   static boolean iconsLoaded = false;
   //An array of all the values.
   static char[] valueList = new char[] {

         'A', '2', '3', '4', '5', '6', '7', '8', '9', 'T', 'J', 'Q', 'K', 'X'

   };
   //An array of all the suits.
   static char[] suitList = new char[] {

         'C', 'D', 'H', 'S'

   };

   //Some methods to change an int into a card value/suit and back again.
   static char turnIntIntoCardValue(int cardValue) {

      return (cardValue < 14) ? valueList[cardValue] : 'M';

   }

   static char turnIntIntoCardSuit(int cardSuit) {

      return (cardSuit < 4) ? suitList[cardSuit] : 'M';

   }

   static int turnCardValueIntoInt(char cardValue) {

      switch (cardValue) {

      case 'A':
         return 0;
      case '2':
         return 1;
      case '3':
         return 2;
      case '4':
         return 3;
      case '5':
         return 4;
      case '6':
         return 5;
      case '7':
         return 6;
      case '8':
         return 7;
      case '9':
         return 8;
      case 'T':
         return 9;
      case 'J':
         return 10;
      case 'Q':
         return 11;
      case 'K':
         return 12;
      case 'X':
         return 13;
      default:
         return 14;

      }

   }

   static int turnCardSuitIntoInt(Card.Suit suit) {

      switch (suit) {

      case CLUBS:
         return 0;
      case DIAMONDS:
         return 1;
      case HEARTS:
         return 2;
      case SPADES:
         return 3;
      default:
         return 4;

      }

   }

   //A method to load the icons.
   static void loadCardIcons() {

      //Set the boolean iconsLoaded to true.
      iconsLoaded = true;

      //Go through the iconCards array, populating it with the file names.
      for (int i = 0; i < 4; i++) {

         for (int j = 0; j < 14; j++) {

            iconCards[j][i] = new ImageIcon("images/" + turnIntIntoCardValue(j) 
            + turnIntIntoCardSuit(i) + ".gif");

         }

      }

      //Set the iconBack to the appropriate ImageIcon.
      iconBack = new ImageIcon("images/BK.gif");
   }

   //A method to getIcon for a particular card.
   static public Icon getIcon(Card card)
   {
      //If the icons are not loaded, load them.
      if (!iconsLoaded)
      {
         loadCardIcons();
      }

      //Return the appropriate icon.
      return iconCards[turnCardValueIntoInt(card.getValue())]
            [turnCardSuitIntoInt(card.getSuit())];

   }

   //A method to get the cardBackIcon.
   static public Icon getCardBackIcon()
   {
      //If the icons are not loaded, load them.
      if (!iconsLoaded)
      {
         loadCardIcons();
      }
      //Return the iconBack.
      return iconBack;

   }



}
//**************************** END GUICARD CLASS *******************************


//***************************** CARD CLASS *************************************
class Card
{
   // contains the suits
   public enum Suit{
      CLUBS,
      DIAMONDS,
      HEARTS,
      SPADES
   }

   // contains the values ('T' stands for 10); get rid of one of these later?
   public static final char[] RANK_VALUES = {'A', '2', '3', '4', '5', '6', '7',
         '8', '9', 'T', 'J', 'Q', 'K', 'X'};
   public static final char [] SUIT_VALUES = {'C', 'D', 'H', 'S'};

   private char value;
   private Suit suit;
   private boolean errorFlag;

   // constructor with no parameters
   public Card()
   {
      // uses the mutator instead of setting the variables directly
      set('A', Suit.SPADES);
   }

   // constructor with both parameters
   public Card(char value, Suit suit)
   {
      set(value, suit);
   }

   // just value
   public Card(char value)
   {
      set(value, Suit.SPADES);
   }

   // just suit
   public Card(Suit suit)
   {
      set('A', suit);
   }

   // convert the contents of this card into a string
   public String toString()
   {
      // return an error if the data is invalid
      if (errorFlag)
         return "[invalid]";

      return (value + " of " + suit.toString().toLowerCase());

   }

   // the mutator
   public boolean set(char value, Suit suit)
   {
      // check to see if it's valid first
      if (isValid(value, suit))
      {
         errorFlag = false;
         this.value = value;
         this.suit = suit;
         return true;
      }
      // still set the data if it's invalid, but set errorFlag to true
      else
      {
         errorFlag = true;
         this.value = value;
         this.suit = suit;
         return false;  
      } 
   }

   // the accessors
   public Suit getSuit()
   {
      return suit;
   }

   public char getValue()
   {
      return value;
   }

   public boolean getErrorFlag()
   {
      return errorFlag;
   }

   // use to compare two cards to each other
   public boolean equals(Card card)
   {
      if ((this.errorFlag == true) || (card.errorFlag == true))
         return false;
      return ((value == card.value) && (suit == card.suit));
   }

   // check the validity of a card
   private boolean isValid(char value, Suit suit)
   {
      for (int counter = 0;(counter < RANK_VALUES.length); ++counter)
      {
         if (RANK_VALUES[counter] == value)
            return true;
      }

      return false;   
   }

   // bubble sort an array of cards
   static void arraySort(Card[] cards, int arraySize)
   {
      int i, j;
      Card swap;
      for (i=0; i<= arraySize; ++i)
      {
         for (j=0; j<= arraySize - 1 - i; ++j)
         {
            if (compareCardValues(cards[j].getValue(), cards[j+1].getValue()
                  ,cards[j].getSuit(), cards[j+1].getSuit()) 
                  == -1)
            {
               swap = cards[j];
               cards[j] = cards[j+1];
               cards[j+1] = swap;
            }
         }
      }
   }

   // helper method to compare two cards' values
   private static int compareCardValues(char value1, char value2, Suit suit1, 
         Suit suit2)
   {
      if (value1 == value2) // values are the same
      {
         if (suit1 == suit2)
            return 0; // the cards are identical
         else
            return compareCardSuits(suit1, suit2); // compare the suits
      }
      else {

         // sequential sort through the array; first char to be found is low
         for(int i=0; i<=RANK_VALUES.length; ++i)
         {

            if (value1 == RANK_VALUES[i])
            {
               return 1; // value1 is lower
            }
            else if (value2 == RANK_VALUES[i])
               return -1; // value2 is lower
         }
      }

      return 0; //this return statement is a failsafe
   }

   // if the values are the same, use this to compare the suits
   private static int compareCardSuits(Suit suit1, Suit suit2)
   {
      char firstSuit;
      char secondSuit;

      // convert the Suit enum into a value that can be compared
      if (suit1 == Suit.CLUBS)
         firstSuit = SUIT_VALUES[0];
      else if (suit1 == Suit.DIAMONDS)
         firstSuit = SUIT_VALUES[1];
      else if (suit1 == Suit.HEARTS)
         firstSuit = SUIT_VALUES[2];
      else  
         firstSuit = SUIT_VALUES[3];

      if (suit2 == Suit.CLUBS)
         secondSuit = SUIT_VALUES[0];
      else if (suit2 == Suit.DIAMONDS)
         secondSuit = SUIT_VALUES[1];
      else if (suit2 == Suit.HEARTS)
         secondSuit = SUIT_VALUES[2];
      else  
         secondSuit = SUIT_VALUES[3];

      // now compare the suits
      for(int i=0; i<=SUIT_VALUES.length; ++i)
      {

         if (firstSuit == SUIT_VALUES[i])
         {
            return 1; // suit1 is lower
         }
         else if (secondSuit == SUIT_VALUES[i])
            return -1; // suit2 is lower
      }
      return 0;
   }
}
//********************************** END CARD CLASS ****************************


//***************************** HAND CLASS *************************************
class Hand {
   //Beginning of the Hand class.

   static int MAX_CARDS = 312;
   //The maximum cards is 312.
   private Card[] myCards = new Card[MAX_CARDS];
   //Fundamental piece of the hand, an array that can be up to 312 cards long.
   private int numCards = 0;
   //The number of cards in hand.

   //A sort function to sort the hand.
   public void sort() {

      if (myCards != null)
         Card.arraySort(myCards, numCards-1);

   }

   public void resetHand() {
      //Beginning of the resetHand function.

      Arrays.fill(myCards, null);
      //Use the Arrays function fill to fill the array myCards with nothing.

   }
   //End of the resetHand function.

   public boolean takeCard(Card card) {
      //Beginning of the takeCard function.

      myCards[numCards] = card;
      //Set the numCardsth position of the myCards array to card.
      numCards++;
      //Increase the number of cards by 1.

      if (myCards[numCards] == card) {
         //If that was successful...

         return true;
         //Return true.

      }
      //Otherwise...

      return false;
      //Return false.

   }
   //End of the takeCard function.



   public Card playCard() {
      //Beginning of the playCard function.

      if(numCards == 0)
         return new Card('B'); //bogus card if no cards in hand

      Card playedCard = myCards[numCards-1];
      //The played card is the last one taken, so the number of cards held -one
      myCards[myCards.length-1] = null;
      //Set the lst card drawn to null.
      numCards--;
      //Subtract one from the number of cards in hand.
      return playedCard;
      //Return the card that was just played.

   }
   //End of the playCard function.


   public Card playCard(int cardIndex) {
      //Beginning of the playCard function.

      if (numCards == 0) {

         return new Card('M', Card.Suit.SPADES);

      }

      Card card = myCards[cardIndex];
      numCards--;
      //Subtract one from the number of cards in hand.
      for (int i = cardIndex; i < numCards; i++) {

         myCards[i] = myCards[i+1];

      }

      myCards[numCards] = null;

      return card;

   }
   //End of the playCard function.

   public String toString() {
      //Beginning of the toString method.

      String theHand = "";
      //A String variable with nothing in it.
      int iterationsSinceNewLine = 0;
      //The number of iterations since a new line was appended onto the end of 
      //theHand.

      for (int i = 0; i < numCards; i++) {
         //For the number of cards...

         if (myCards[i] != null) {
            //If the ith slot is not empty...

            theHand += myCards[i] + " / ";
            //Append the ith card and a slash seperated by spaces onto theHand.
            iterationsSinceNewLine++;
            //Increase the number of iterations since a new line was appended
            //onto the end of theHand.

         }
         //end of if.

         if (iterationsSinceNewLine > 4) {
            //If he number of iterations since a new line was appended onto the
            //end of theHand is more than 4...

            theHand += "\n";
            //Append a new line onto theHand.
            iterationsSinceNewLine = 0;
            //Reset the number of iterations since a new line was appended onto
            //the end of theHand.

         }
         //End of if.

      }
      //End of for.

      return theHand;
      //Return theHand.

   }
   //End of the toString method.

   public int getNumCards() {
      //Beginning of the getNumCards method.

      return numCards;
      //Return numCards.

   }
   //End of the getNumCards method.

   Card inspectCard(int k) {
      //Beginning of the inspectCard method.

      Card badCard = new Card('M');
      //Create a card with errorFlag being true.

      try {
         //Try to do the following...

         if (myCards[k] == null) {
            //If there is not a card at the position being checked...

            return badCard;
            //Return badCard.

         }
         //End of if.

         return myCards[k];
         //Return the card at the position specified.

      }
      //End of try.
      catch (ArrayIndexOutOfBoundsException e) {
         //If that could not be done due to an ArrayIndexOutOfBoundsException...

         return badCard;
         //Return badCard.

      }
      //End of catch.

   }
   //End of the inspectCard method.

}
//End of the class.
//********************************** END HAND CLASS ****************************


//***************************** DECK CLASS *************************************
class Deck
{
   public static final int MAX_CARDS = 6; // Max number of decks/packs
   public static final int DEFAULT_NUM_PACKS = 1;
   public static final int DECK_SIZE = GUICard.NUM_RANKS*GUICard.NUM_SUITS;
   public static final int SHUFFLE_VAL = 7; //# of times to shuffle each deck
   private static Card[] masterPack;
   private static int masterPackTopCard = 0;
   private Card[] cards;
   private int topCard; // the current array index of the top card

   public Deck() // default constructor for no packs given
   {
      allocateMasterPack();

      int numPacks = DEFAULT_NUM_PACKS;

      cards = new Card[numPacks*DECK_SIZE];

      init(numPacks);
   }

   public Deck(int numPacks) //constructor with numPacks parameter
   {
      allocateMasterPack();

      if ((numPacks < 1) || (numPacks > MAX_CARDS))
         numPacks = DEFAULT_NUM_PACKS;

      cards = new Card[numPacks*DECK_SIZE];

      init(numPacks);


   }

   public void init(int numPacks)
   {
      Boolean firstCard = true;
      topCard = 0; //set top card to bottom

      if ((numPacks < 1) || (numPacks > MAX_CARDS)) // Validate number of packs
         numPacks = DEFAULT_NUM_PACKS;

      //loop through number of packs, adding all masterPack items each time
      for (int j = 0; j < numPacks; j++)
      {
         for (int k = 0; k < DECK_SIZE; k++)
         {

            if ((firstCard))
               firstCard = false;
            else
               topCard++;

            cards[topCard] = new Card(masterPack[k].getValue(), 
                  masterPack[k].getSuit());
         }
      }

   }

   public void init()  //overloaded init() for no numPacks, setting to default
   {
      topCard = 0; //set top card to bottomg

      int numPacks = DEFAULT_NUM_PACKS;

      //loop through number of packs, adding all masterPack items each time
      for (int j = 0; j < numPacks; j++)
      {
         for (int k = 0; k < DECK_SIZE; k++)
         {
            //used to be if (topCard != 0, not k
            if (k != 0)
               topCard++;

            cards[topCard] = new Card(masterPack[k].getValue(),
                  masterPack[k].getSuit());
         }
      }

   }

   public void shuffle()
   {
      //A temporary card reference value to allow for switching around cards
      Card tempCard;
      Random rand = new Random();
      int randInt; //value to hold temporary random int

      // loop through the whole deck SHUFFLE_VAL times, storing a card in temp 
      //value, riffling by randomly assigning that spot to another card in the
      //deck, and then sending the original card to that spot
      for(int j = 0; j < SHUFFLE_VAL; j++)
      {
         for (int k = 0; k <= topCard; k++) //this is a single riffle
         {
            randInt = rand.nextInt(topCard-1);
            tempCard = cards[k];
            cards[k]= cards[randInt];
            cards[randInt] = tempCard;
         }
      }
   }

   public void shuffle(int numDecks) //overloading to allow for multiple decks
   {
      //A temporary card reference value to allow for switching around cards
      Card tempCard;
      Random rand = new Random();
      int randInt; //value to hold temporary random int

      // loop through the whole deck numDeck*SHUFFLE_VAL times, storing a 
      //card in temp value, riffling by randomly assigning that spot to another 
      //card in the deck, and then sending the original card to that spot
      for(int j = 0; j < numDecks*SHUFFLE_VAL; j++)
      {
         for (int k = 0; k <= topCard; k++) //this is a single riffle
         {
            randInt = rand.nextInt(topCard-1);
            tempCard = cards[k];
            cards[k]= cards[randInt];
            cards[randInt] = tempCard;
         }
      }
   }

   public Card dealCard()
   {     
      //return invalid card if trying to deal from empty deck
      if (cards[topCard] == null)
         return new Card('B');

      //create a temp value to reference the actual card in the array
      Card returnCard = cards[topCard];

      //set cards[0] to null if this is the last one 
      if(topCard == 0)
         cards[topCard] = null;
      else
         //lower topCard to allow the reference from the array to be overwritten
         topCard--;

      return returnCard;
   }

   private static void allocateMasterPack()
   {
      if (masterPackTopCard != 0) //kick back if masterPack isn't empty
         return;

      masterPack = new Card[DECK_SIZE]; //initiate the pack

      //loop through all card values, adding one card of each suit on each loop
      //use masterPackTopCard to keep track of where we are in the pack
      for (int j = 0; j < Card.RANK_VALUES.length; j++)
      {
         for (Card.Suit suit : Card.Suit.values())
         {
            masterPack[masterPackTopCard] = new Card(Card.RANK_VALUES[j], suit);
            masterPackTopCard++;

         }
      }
      return;      
   }

   public int getTopCard()
   {
      return topCard;
   }

   public Card inspectCard(int k)
   {
      if (k<=topCard)
         return cards[k];
      else
         return new Card('B'); //return invalid card

   }

   public boolean addCard(Card card)
   {   
      if (topCard >= cards.length - 1)
         return false; //array is full, cannot add

      int cardInstances = 0;

      int numPacks = (int)Math.ceil(((double)topCard)/((double)DECK_SIZE));

      for (int k = 0; k <= topCard; k++)
      {
         if (cards[k].equals(card))
         {
            cardInstances++;
         }
      }

      if (cardInstances > numPacks)
         return false;

      topCard++;
      cards[topCard] = new Card(card.getValue(), card.getSuit());

      return true;
   }

   public boolean removeCard(Card card)
   {
      boolean foundCard = false;

      if ((topCard == 0) && (cards[topCard] == null)) //error
         return false;

      if ( topCard == 0 ) 
      {
         cards[topCard] = null;
         return true;
      }

      for (int k = 0; k <= topCard; k++)
      {
         if (cards[k].equals(card))
         {
            cards[k] = cards[topCard];
            foundCard = true;

            if ( topCard == 0 ) 
               cards[topCard] = null;
            else
               topCard--;
         }
      }

      return foundCard;
   }

   public int getNumCards()
   {
      return (topCard + 1);
   }

   public void sort()
   {
      if (cards != null)
         Card.arraySort(cards, topCard);
   }

   public void display() //only for debugging
   {
      System.out.println();
      for (int i = 0; i <= topCard; i++)
      {
         System.out.print(cards[i].toString() + "      ");

         if ((i+1)%4 == 0)
            System.out.println();
      }
   }
}
//****************************END DECK CLASS************************************