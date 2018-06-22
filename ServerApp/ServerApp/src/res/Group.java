/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package res;

import java.util.ArrayList;

/**
 *
 * @author define
 */
public class Group {
    public int admin_id;
    public String adminName;
    public String title;
    public String desc;
    public String created_on;
    public ArrayList<ClientDetails> active;
    public ArrayList<ChatDetails> chat;
}
