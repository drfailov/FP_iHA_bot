package com.fsoft.ihabot.answer;

import com.fsoft.ihabot.communucation.tg.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;

/* * Какая инфа про ответ должна хранится:
 * - ID ответа (id, long. В случае коллизий генерировать новые)
 * - Message вопроса (текст вопроса, автор вопроса, дата вопроса)
 * - Message ответа (текст ответа, автор ответа, дата ответа)
* */
public class AnswerElement {
    private long id = 0; //В случае коллизий генерировать новые)
    private Message questionMessage = null;
    private Message answerMessage = null;
    private int timesUsed = 0; //Количество раз, сколько раз бот использовал этот ответ

    public AnswerElement() {
    }
    public AnswerElement(JSONObject jsonObject)throws JSONException, ParseException {
        fromJson(jsonObject);
    }

    public Message getQuestionMessage() {
        return questionMessage;
    }

    public Message getAnswerMessage() {
        return answerMessage;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", id);
        jsonObject.put("timesUsed", timesUsed);
        if(questionMessage != null)
            jsonObject.put("questionMessage", questionMessage.toJson());
        if(answerMessage != null)
            jsonObject.put("answerMessage", answerMessage.toJson());
        return jsonObject;
    }

    private void fromJson(JSONObject jsonObject)throws JSONException, ParseException {
        if(jsonObject.has("id"))
            id = jsonObject.getLong("id");
        if(jsonObject.has("timesUsed"))
            timesUsed = jsonObject.getInt("timesUsed");

        if(jsonObject.has("questionMessage"))
            questionMessage = new Message(jsonObject.getJSONObject("questionMessage"));

        if(jsonObject.has("answerMessage"))
            answerMessage = new Message(jsonObject.getJSONObject("answerMessage"));
    }
}
