JSONObject json;

void setup() {
  println("Start...");
  BufferedReader reader;
  PrintWriter output;
  String line = "";
  long counter = 0;
  //reader = createReader("data/answer_database_old.txt");
  reader = createReader("data/answer_database_old_full.txt");
  output = createWriter("data/answer_database.txt"); 
  
  try {
    while(line != null){
      line = reader.readLine();
      if(line == null || line.isEmpty()){
        
        println("Finished at ", counter);
        break;
      }
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
      
      
      JSONObject jsonOut_questionMessage = new JSONObject();
      jsonOut_questionMessage.setString("text", jsonIn_questionText);
      jsonOut_questionMessage.setString("date", jsonIn_questionDate);
      jsonOut_questionMessage.setJSONObject("author", authorByVkId(jsonIn_questionAuthor_id));
      
      
      JSONObject jsonOut_answerMessage = new JSONObject();
      jsonOut_answerMessage.setString("text", jsonIn_answerText);
      jsonOut_answerMessage.setString("date", jsonIn_createdDate);
      jsonOut_answerMessage.setJSONObject("author", authorByVkId(jsonIn_createdAuthor_id));
      
      
      JSONArray jsonOut_attachments = new JSONArray();
      for(int i=0; i<answerAttachments_types.size(); i++){
        JSONObject attachment = new JSONObject();
        attachment.setString("type", answerAttachments_types.get(i));
        attachment.setString("file", answerAttachments_files.get(i));
        jsonOut_attachments.setJSONObject(i, attachment);
      }
     
      
      JSONObject jsonOut = new JSONObject();
      jsonOut.setLong("id", counter);   
      jsonOut.setLong("timesUsed", jsonIn_timesUsed);   
      jsonOut.setJSONObject("questionMessage", jsonOut_questionMessage);
      jsonOut.setJSONObject("answerMessage", jsonOut_answerMessage);
      jsonOut.setJSONArray("attachments", jsonOut_attachments);
      
      output.println(
        jsonOut.toString()
        .replace("\t", "")
        .replace("\n", "")
        .replace("   ", " ")
        .replace("  ", " ")
        .replace("{ ", "{")
      );
      counter++;      
    }
  } catch (Exception e) {
    e.printStackTrace();
    line = null;
    println(e);
    println("EOF наверное");
  }
  
  println("Close file...");
  output.flush(); // Writes the remaining data to the file
  output.close(); // Finishes the file
  exit();
}


JSONObject authorByVkId(long jsonIn_questionAuthor_id){
  
      JSONObject jsonOut_questionMessage_author = new JSONObject();
      if(jsonIn_questionAuthor_id == 10299185){ //Dr. Failov VK ID
        //"id":248067313,"is_bot":false,"first_name":"Dr","last_name":"Failov","username":"DrFailov","language_code":"ru"
        jsonOut_questionMessage_author.setString("username", "drfailov");
        jsonOut_questionMessage_author.setLong("id", 248067313);
        jsonOut_questionMessage_author.setString("first_name", "Dr");
        jsonOut_questionMessage_author.setString("last_name", "Failov");
        jsonOut_questionMessage_author.setString("language_code", "ru");
      }     
      else if(jsonIn_questionAuthor_id == 262949329){ //Cyber Tailor VK ID
        //"id":1601776521,"is_bot":false,"first_name":"Cyber","last_name":"Tailor","username":"cybertailor","language_code":"ru"
        jsonOut_questionMessage_author.setString("username", ""); //cybertailor
        jsonOut_questionMessage_author.setLong("id", 0); //1601776521
        jsonOut_questionMessage_author.setString("first_name", "Cyber");
        jsonOut_questionMessage_author.setString("last_name", "Tailor");
        jsonOut_questionMessage_author.setString("language_code", "ru");
      }
      else if(jsonIn_questionAuthor_id == 140830142){ //Олег Плаксин VK ID
        //
        jsonOut_questionMessage_author.setString("username", ""); //plaxeen
        jsonOut_questionMessage_author.setLong("id", 0);
        jsonOut_questionMessage_author.setString("first_name", "Олег");
        jsonOut_questionMessage_author.setString("last_name", "Плаксин");
        jsonOut_questionMessage_author.setString("language_code", "ru");
      }
      else{
        println("UNKNOWN ID", jsonIn_questionAuthor_id);
        jsonOut_questionMessage_author.setString("username", "");
        jsonOut_questionMessage_author.setLong("id", 0);
        jsonOut_questionMessage_author.setString("first_name", "No");
        jsonOut_questionMessage_author.setString("last_name", "Name");
        jsonOut_questionMessage_author.setString("language_code", "ru");
      }
      return jsonOut_questionMessage_author;
}
