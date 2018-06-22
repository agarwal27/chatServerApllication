/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package serverapp;

import java.net.*;
import java.io.*;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import res.ChatDetails;
import res.ClientDetails;
import res.Group;

/**
 *
 * @author define
 */
public class ClientHandler extends Thread{
    private Socket client;
    private String id,name;
    public ClientHandler(Socket client){
        this.client=client;
        this.start();
    }
    
    public void run(){
        try{
            while(true){
                ObjectInputStream in=new ObjectInputStream(this.client.getInputStream());
                String req=in.readObject().toString();
                if(req.equals("LoginDetails")){
                    String logid=in.readObject().toString();
                    String pass=in.readObject().toString(); 
                    String query="select  * from User_Master where LOGIN_ID='" + logid + "' and PASSWORD='" + pass + "'";
                    ResultSet rs=res.ConnectionFactory.getInstance().getResultSet(query);
                    ObjectOutputStream out=new ObjectOutputStream(this.client.getOutputStream());
                    
                    if(rs.next()){
                        out.writeObject("Success");
                        this.id=rs.getInt(1) + "";
                        this.name=rs.getString(4);
                        
                        String status=name + " logged in at " + res.CommRes.getDateTime(Calendar.getInstance());
                        res.CommRes.serverwin.setNetworkStatus(status);
                        
                        res.ClientDetails details=new res.ClientDetails();
                        details.id=this.id;
                        details.client=this.client;
                        res.CommRes.onlineUsers.add(details);
                        
                        
                        
                    }else{
                        out.writeObject("Failed");
                    }
                }
                if(req.equals("NewGroup")){
                    String grpName=in.readObject().toString();
                    String grpDesc=in.readObject().toString();
                    
                    Group group=new Group();
                    group.admin_id= Integer.parseInt(this.id);
                    group.adminName=this.name;
                    group.title=grpName;
                    group.desc=grpDesc;
                    group.created_on=res.CommRes.getDateTime(Calendar.getInstance());
                    group.active=new ArrayList<ClientDetails>();
                    group.chat=new ArrayList<ChatDetails>();
                    ClientDetails details=new ClientDetails();
                    details.id=this.id;
                    details.client=this.client;
                    group.active.add(details);
                    
                    res.CommRes.groups.add(group);
                    
                    for(int i=0;i<res.CommRes.onlineUsers.size();i++){
                        ClientDetails cdetails=res.CommRes.onlineUsers.get(i);
                        ObjectOutputStream tmp=new ObjectOutputStream(cdetails.client.getOutputStream());
                        tmp.writeObject("NewGroup");
                        tmp.writeObject(group);
                    }
                    
                }
                if(req.equals("")){
                    
                }
                if(req.equals("")){
                    
                }
                if(req.equals("")){
                    
                }
            }
        }catch(Exception ex){
            
        }
    }
}
