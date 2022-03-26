import java.util.Hashtable;
import java.util.Map;
import java.util.Enumeration;

JSONObject json;
Hashtable hashtable = new Hashtable();

void setup() {
  println("Start...");
  BufferedReader reader;  
  PrintWriter output;
  String line = "";
  long counter = 0;
  
  println("Open IN file...");
  reader = createReader("data/answer_database_old_full.txt"); 
  
  try {
    while(line != null){
      line = reader.readLine();
      if(line == null || line.isEmpty()){
        
        println("Finished at ", counter);
        break;
      }
      
      counter++;  
      //println(counter++, jsonIn.toString().replace("\t", "").replace("\n", "").replace("  ", " ").substring(0,110), "...");
      JSONObject jsonIn = parseJSONObject(line);
      String jsonIn_questionText = jsonIn.getString("questionText");   
      String jsonIn_answerText = jsonIn.getString("answerText");     
      JSONObject jsonIn_questionAuthor = jsonIn.getJSONObject("questionAuthor");
      long jsonIn_questionAuthor_id = jsonIn_questionAuthor.getLong("id");
      JSONObject jsonIn_createdAuthor = jsonIn.getJSONObject("createdAuthor");
      long jsonIn_createdAuthor_id = jsonIn_createdAuthor.getLong("id");
      String jsonIn_questionDate = jsonIn.getString("questionDate");
      String jsonIn_createdDate = jsonIn.getString("createdDate");
      long jsonIn_timesUsed = jsonIn.getLong("timesUsed");
      JSONArray jsonIn_answerAttachments = jsonIn.getJSONArray("answerAttachments");
      ArrayList<String> answerAttachments_types = new ArrayList<>();
      ArrayList<String> answerAttachments_files = new ArrayList<>();
      for(int i=0; i<jsonIn_answerAttachments.size(); i++){
        JSONObject attachment = jsonIn_answerAttachments.getJSONObject(i); 
        answerAttachments_types.add(attachment.getString("type"));
        answerAttachments_files.add(attachment.getString("file"));
      }
      
      String question = jsonIn_questionText.toLowerCase().trim();
      question = filterSymbols(question);
      question = question.replace(" +", " ");
      String[] words = question.split(" ");
      
      if(counter % 145 == 0)
        println("Processing ...", counter);
      
      for(int i=0; i<words.length; i++){
        int freq = (Integer)hashtable.getOrDefault(words[i], 0);
        freq++;
        hashtable.put(words[i], freq);
      }
      
          
    }
  } catch (Exception e) {
    e.printStackTrace();
    line = null;
    println(e);
    println("EOF наверное");
  }
  
  println("Close IN file...");
  


  println("Open OUT file...");
  output = createWriter("data/words_frequency.txt");
        
  Enumeration<String> e = hashtable.keys();
  int total = 0;
  while (e.hasMoreElements()) {
 
      // Getting the key of a particular entry
      String word = e.nextElement();
      int freq = (Integer)hashtable.get(word);
      if(freq > 20){
        println(freq, word);
        output.println(freq + "\t" + word);
        total ++;
      }
  }
  
  
  
  output.flush(); // Writes the remaining data to the file
  output.close(); // Finishes the file
  println("Wrote lines: ", total);
  println("Close OUT file...");
  exit();
}

String filterSymbols(String input){
    String allowedSymbols = "qwertyuiopasdfghjklzxcvbnm їіёйцукенгшщзхъфывапролджэячсмитьбю 1234567890";
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < input.length(); i++) {
        char c = input.charAt(i);
        if(allowedSymbols.indexOf(c) >= 0)
            builder.append(c);
    }
    return builder.toString();
}
