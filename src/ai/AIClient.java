package ai;

import ai.Global;
import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.*;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import kalaha.*;

/**
 * This is the main class for your Kalaha AI bot. Currently
 * it only makes a random, valid move each turn.
 * 
 * @author Johan Hagelbäck
 */
public class AIClient implements Runnable
{
    private int player;
    private JTextArea text;
    
    private PrintWriter out;
    private BufferedReader in;
    private Thread thr;
    private Socket socket;
    private boolean running;
    private boolean connected;
    	
    /**
     * Creates a new client.
     */
    public AIClient()
    {
	player = -1;
        connected = false;
        
        //This is some necessary client stuff. You don't need
        //to change anything here.
        initGUI();
	
        try
        {
            addText("Connecting to localhost:" + KalahaMain.port);
            socket = new Socket("localhost", KalahaMain.port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            addText("Done");
            connected = true;
        }
        catch (Exception ex)
        {
            addText("Unable to connect to server");
            return;
        }
    }
    
    /**
     * Starts the client thread.
     */
    public void start()
    {
        //Don't change this
        if (connected)
        {
            thr = new Thread(this);
            thr.start();
        }
    }
    
    /**
     * Creates the GUI.
     */
    private void initGUI()
    {
        //Client GUI stuff. You don't need to change this.
        JFrame frame = new JFrame("My AI Client");
        frame.setLocation(Global.getClientXpos(), 445);
        frame.setSize(new Dimension(420,250));
        frame.getContentPane().setLayout(new FlowLayout());
        
        text = new JTextArea();
        JScrollPane pane = new JScrollPane(text);
        pane.setPreferredSize(new Dimension(400, 210));
        
        frame.getContentPane().add(pane);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        frame.setVisible(true);
    }
    
    /**
     * Adds a text string to the GUI textarea.
     * 
     * @param txt The text to add
     */
    public void addText(String txt)
    {
        //Don't change this
        text.append(txt + "\n");
        text.setCaretPosition(text.getDocument().getLength());
    }
    
    /**
     * Thread for server communication. Checks when it is this
     * client's turn to make a move.
     */
    public void run()
    {
        String reply;
        running = true;
        
        try
        {
            while (running)
            {
                //Checks which player you are. No need to change this.
                if (player == -1)
                {
                    out.println(Commands.HELLO);
                    reply = in.readLine();

                    String tokens[] = reply.split(" ");
                    player = Integer.parseInt(tokens[1]);
                    
                    addText("I am player " + player);
                }
                
                //Check if game has ended. No need to change this.
                out.println(Commands.WINNER);
                reply = in.readLine();
                if(reply.equals("1") || reply.equals("2") )
                {
                    int w = Integer.parseInt(reply);
                    if (w == player)
                    {
                        addText("I won!");
                    }
                    else
                    {
                        addText("I lost...");
                    }
                    running = false;
                }
                if(reply.equals("0"))
                {
                    addText("Even game!");
                    running = false;
                }

                //Check if it is my turn. If so, do a move
                out.println(Commands.NEXT_PLAYER);
                reply = in.readLine();
                if (!reply.equals(Errors.GAME_NOT_FULL) && running)
                {
                    int nextPlayer = Integer.parseInt(reply);

                    if(nextPlayer == player)
                    {
                        out.println(Commands.BOARD);
                        String currentBoardStr = in.readLine();
                        boolean validMove = false;
                        while (!validMove)
                        {
                            long startT = System.currentTimeMillis();
                            //This is the call to the function for making a move.
                            //You only need to change the contents in the getMove()
                            //function.
                            GameState currentBoard = new GameState(currentBoardStr);
                            int cMove = getMove(currentBoard);
                            
                            //Timer stuff
                            long tot = System.currentTimeMillis() - startT;
                            double e = (double)tot / (double)1000;
                            
                            out.println(Commands.MOVE + " " + cMove + " " + player);
                            reply = in.readLine();
                            if (!reply.startsWith("ERROR"))
                            {
                                validMove = true;
                                addText("Made move " + cMove + " in " + e + " secs");
                            }
                        }
                    }
                }
                
                //Wait
                Thread.sleep(100);
            }
	}
        catch (Exception ex)
        {
            running = false;
        }
        
        try
        {
            socket.close();
            addText("Disconnected from server");
        }
        catch (Exception ex)
        {
            addText("Error closing connection: " + ex.getMessage());
        }
    }
    
    
    
    
     /**
     * Getting the score difference between players.    
     * @param currentBoard The current GameState.
     * @param player The current players turn.
     * @return Score difference between players with the current GameState.
     */
    public int getScoreDifference(GameState currentBoard, int player)
    {
       if(player == 1) {
           return (currentBoard.getScore(1) - currentBoard.getScore(2));
       }
       else {
           return (currentBoard.getScore(2) - currentBoard.getScore(1));
       }    
    }
    
    
     /**
     * The minmax Algoritm that calculates the best move according to getScoreDifference.
     * Using alpha beta pruning with max time of 5s.  
     *
     * 
     * @param depth The search depth.
     * @param alpha Current alpha value.
     * @param beta Current beta value.      
     * @param player Maximum player value. 
     * @param startTime Start time of the whole iterative deep search.
     * @param time time since the start of the iterative deep search.  
     * @param currentBoard The current GameState
     * @return Best move with this depth usage.
     */
    int minimax(GameState currentState, int depth, int alpha, int beta, int player,long startTime, double time,float timelimit)
    {
        int eval;
        int maxEval = Integer.MIN_VALUE;
        int minEval = Integer.MAX_VALUE;
        
        if (depth == 0 && !currentState.gameEnded())
        {
            //on lowest depth. return the score of this node.     
            return getScoreDifference(currentState.clone(),player);
        
        }
        else if(currentState.gameEnded())
        {
           //if game is over here, get the winner
            if(currentState.getWinner() == player)
            {
               return 500;
            } 
            else
            {
               return -500;
            }            
        }
        // Maximizing player
        if (currentState.getNextPlayer() == player)
        {
            for (int i = 1; i < 7; i++)
            {
                //check if move is possible.
                if(currentState.moveIsPossible(i))
                {
                    GameState temp = currentState.clone();
                    temp.makeMove(i);
                    eval = minimax(temp,depth-1,alpha,beta,player,startTime,time,timelimit);
                    //check if new value is better then last best value.

                    if(eval > maxEval)
                    {   
                        maxEval = eval;                        
                    }
                    
                    //Update timing check to not overexed 5s.
                    long totalTime = System.currentTimeMillis() - startTime;
                    time += (double)totalTime / 1000;
                    
                    if (time >= timelimit) 
                    {
                        //Time is out, exit from algoritm.
                        break;
                    }
                    
                    //alpha beta pruning code part.
                    alpha = Math.max(alpha, maxEval);                  
                    if (beta <= alpha)
                    {
                        break;
                    }
                }
            }
            return maxEval;
        }
        //mini player
        else
        {
           
            for (int i = 1; i < 7; i++)
            {
                //check if move is possible.                
                if(currentState.moveIsPossible(i))
                {
                    GameState temp = currentState.clone();
                    temp.makeMove(i);
                    
                     eval = minimax(temp,depth-1,alpha,beta,player,startTime,time,timelimit);
                    //check if new value is better then last best value.
                    if(eval < minEval)
                    {
                        minEval = eval;
                        
                    }
                     //Update timing check to not overexed 5s.
                    long totalTime = System.currentTimeMillis() - startTime;
                    time += (double)totalTime / 1000;
                    
                    if (time >= timelimit) 
                    {
                        //Time is out, exit from algoritm.
                        break;
                    }
                    
                    //alpha beta pruning code part.                 
                    beta = Math.min(beta, minEval);  
                    if (beta <= alpha)
                    {
                        break;
                    }
                   
                }
            }
            return minEval; 
        }
    }
    
    /**
     * The main function of iterativeDeepning search. Runs for 0.833 seconds 
     * for each iterative, and iteratives 6 times (Each possible move this turn).
     * Results in a total time of 5s for whole search.
     *
     * 
     * @param depth The first search depth
     * @param currentBoard The current GameState
     * @return Best move this turn
     */
    public int iterDeep(int depth, GameState currentBoard)
    {
        //start depth of the iteration depth search.
        int currentDepth = depth;
        double time = 0.0;
        int currentScore = Integer.MIN_VALUE;
        int currentMove = -1;
        
        //step though each possible step for 0.83s as deep as you can.
        int amountofSteps = 0;
        for(int i = 1; i < 7; i++)
        {
            if(currentBoard.moveIsPossible(i))
            {
                amountofSteps++;
            }
        }
        float timelimit = 4.99f / amountofSteps;
        
        if(amountofSteps == 1){
            for(int i = 1; i < 7; i++)
            {
                if(currentBoard.moveIsPossible(i))
                {
                    return i;
                }
            }
        }
        for(int i = 1; i < 7; i++)
        {
            time = 0.0;
            currentDepth = depth;
            while(true)
            {
                long startTime = System.currentTimeMillis();
                if(currentBoard.moveIsPossible(i))
                {
                    //makes the move and sees the max score if doing this move.
                    GameState temp = currentBoard.clone();
                    temp.makeMove(i);
                    int newScore = 0;        
                    newScore = minimax(temp, currentDepth, Integer.MIN_VALUE, Integer.MAX_VALUE,currentBoard.getNextPlayer(),startTime,time,timelimit);
                    //check if new move is better than current move.
                    if(newScore >= currentScore)
                    {
                        currentScore = newScore;
                        currentMove = i;
                    }
                    //update time check.
                    //add depth, there is more time to go!
                    currentDepth += 2;
                }
                else
                {
                    break;
                }
                long totalTime = System.currentTimeMillis() - startTime;
                time += (double)totalTime / 1000;
                if (time >= timelimit) 
                {
                    //time is out exit from algoritm.
                    break;
                }
            }
        }
        /*GameState tempstate = currentBoard.clone();
        tempstate.makeMove(currentMove);
        System.out.println(getScoreDifference(tempstate,currentBoard.getNextPlayer()));*/
        
        return currentMove;
    }
      

     /**
     * This is the function that reads the first moves statistics from file 
     * and makes a 2d array 
     *
     * 
     * @return 2d array of stats, [0-6][0] amount played and [0-6][1] is amount won
     */
    public int[][] readStats(){
        int[][] temp = new int[6][2];
        try {
            File myObj = new File("Stats.txt");
            Scanner myReader = new Scanner(myObj);
            int i = 0;
            while (myReader.hasNextLine()) {
            if(i > 6){
                break;
            }
            String data = myReader.nextLine();
            String[] split = data.split(" ");
            //split string to two ints.
            int games = Integer.parseInt(split[0]);
            int wins = Integer.parseInt(split[1]);
            temp[i][0] = games;
            temp[i][1] = wins;
            i++;          
            }
            myReader.close();
        } catch (IOException ex) {
            Logger.getLogger(AIClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return temp;
    }
    
    
     /**
     * This is the function that saves the first moves statistics to
     * Another file
     * 
     * @param stats The updated stats of the first moves
     */
    public void writeStats(int[][] stats){
        try {
            FileWriter myWriter = new FileWriter("Stats.txt");
            for(int i = 0; i < 6; i++){
                myWriter.write(stats[i][0] + " " + stats[i][1] + "\n");
            }
            myWriter.close();
        } catch (IOException ex) {
            Logger.getLogger(AIClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    boolean firstMove = true;
    int bestStartMove = -1;
    
     /**
     * This is the method that makes a move each time it is your turn.
     * Here you need to change the call to the random method to your
     * Minimax search.
     * 
     * @param currentBoard The current board state
     * @return Move to make (1-6)
     */
    public int getMove(GameState currentBoard)
    {   
        int move = -1;
        //check if this is the first move.
        if(firstMove){
            //load stats of first moves.
            int[][] stats = readStats();                  
            float highestOdds = -1.0f;
            //check odds of wining with the stats of first moves.
            for(int i = 0; i < 6; i++){
                if(stats[i][0] > 0){
                    float odds = ((float)stats[i][1]/(float)stats[i][0]);
                    if(odds > highestOdds){
                        bestStartMove = i+1;
                        highestOdds = odds;
                    }
                }
            }
            if(bestStartMove == -1){
                //if you dont got the file, dont worry i will do a 
                //iterative depth check for you <3
                bestStartMove = iterDeep(4,currentBoard);
            }
            move = bestStartMove;
            stats[bestStartMove - 1][0]++;
            //write to file what move we used incase we lose.
            writeStats(stats);
            firstMove = false;
        }else{
            //its not our first move, check with iterative depth search.
            move = iterDeep(7,currentBoard);      
        }
        
        //check if this is the game winning move and record a Epic battleroyal win!
        //Winner winner chicken dinner!!! *Epic fortnite dance*
        GameState winTest = currentBoard.clone();
        winTest.makeMove(move);
        if(winTest.getScore(currentBoard.getNextPlayer()) > 37){         
            //we won and record this starting move as a win. writing to file.
            int[][] stats = readStats();                  
            stats[bestStartMove - 1][1]++;  
            writeStats(stats);           
        }
        
        return move; 
    }
    
    /**
     * Returns a random ambo number (1-6) used when making
     * a random move.
     * 
     * @return Random ambo number
     */
    public int getRandom()
    {
        return 1 + (int)(Math.random() * 6);
    }
    
    
}