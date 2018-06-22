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
                        this.id=rs.getInt(1) + "";
                        this.name=rs.getString(4);
                        out.writeObject("Success");
                        out.writeObject(this.id);
                        out.writeObject(this.name);
                        out.writeObject(res.CommRes.groups);
                        
                        
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
                    
                    File file=new File("GroupInfo/" + grpName);
                    file.mkdir();
                    
                    Group group=new Group();
                    group.admin=new ClientDetails();
                    group.admin.id= this.id;
                    group.admin.client=this.client;
                    group.admin.name=this.name;
                    group.title=grpName;
                    group.desc=grpDesc;
                    group.created_on=res.CommRes.getDateTime(Calendar.getInstance());
                    group.active=new ArrayList<ClientDetails>();
                    group.chat=new ArrayList<ChatDetails>();
                    group.requests=new ArrayList<ClientDetails>();
                    ClientDetails details=new ClientDetails();
                    details.id=this.id;
                    details.name=this.name;
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
                if(req.equals("JoinRequest")){
                   int  index=Integer.parseInt(in.readObject().toString());
                    ClientDetails details=new ClientDetails();
                    details.id=this.id;
                    details.client=this.client;
                    details.name=this.name;
                    
                    Group group=res.CommRes.groups.get(index);
                    group.requests.add(details);
                    ObjectOutputStream out=new ObjectOutputStream(group.admin.client.getOutputStream());
                    out.writeObject("JoinRequest");
                    out.writeObject(index + "");
                    out.writeObject(details);
                }
                if(req.equals("accept")){
                   int grpindex=Integer.parseInt(in.readObject().toString()); 
                   int userindex=Integer.parseInt(in.readObject().toString());
                   Group group=res.CommRes.groups.get(grpindex);
                   ClientDetails details=group.requests.get(userindex);
                   group.active.add(details);
                   group.requests.remove(userindex);
                   for(int i=0;i<res.CommRes.onlineUsers.size();i++)
                   {
                    ClientDetails xdetails=res.CommRes.onlineUsers.get(i);
                    ObjectOutputStream tmp=new ObjectOutputStream(xdetails.client.getOutputStream());
                    tmp.writeObject("Accept");
                    tmp.writeObject(grpindex);
                    tmp.writeObject(group);
                   }
                }
                
                //deleting group
                
                if(req.equals("GroupDel")){
                    int index=Integer.parseInt(in.readObject().toString());
                    String grpName=res.CommRes.groups.get(index).title;
                    res.CommRes.groups.remove(index);
                    for(int i=0;i<res.CommRes.onlineUsers.size();i++){
                        ClientDetails cdetails=res.CommRes.onlineUsers.get(i);
                        ObjectOutputStream tmp=new ObjectOutputStream(cdetails.client.getOutputStream());
                        tmp.writeObject("GroupDel");
                        tmp.writeObject(index+"");
                        tmp.writeObject(grpName);
                    }
                    
                }
                
                //exiting from group
                
                if(req.equals("GroupExit")){
                    int index=Integer.parseInt(in.readObject().toString());
                    int member_id=Integer.parseInt(this.id);
                    System.out.println("Active member id"+member_id);
                    Group group=res.CommRes.groups.get(index);
                    for(int i=0;i<group.active.size();i++){System.out.println(group.active.get(i).id);}
                    for(int i=0;i<group.active.size();i++){
                        int m_id=Integer.parseInt(group.active.get(i).id);
                      if(member_id==(m_id)){
                          System.out.println("Active member match"+member_id);
                        int pos=i;
                        res.CommRes.groups.get(index).active.remove(pos);
                        System.out.println("Active member remove"+member_id);
                        break;
                      }
                    }
                    for(int i=0;i<res.CommRes.onlineUsers.size();i++){
                        ClientDetails cdetails=res.CommRes.onlineUsers.get(i);
                        ObjectOutputStream tmp=new ObjectOutputStream(cdetails.client.getOutputStream());
                        tmp.writeObject("GroupExit");
                        tmp.writeObject(index+"");
                        tmp.writeObject(member_id+"");
                    }
                    
                }
                if(req.equals("ChatData")){
                    int grpId=Integer.parseInt(in.readObject().toString());
                    int index=Integer.parseInt(in.readObject().toString());
                    String path=in.readObject().toString();
                    Group group=res.CommRes.groups.get(grpId);
                    ChatDetails chat=group.chat.get(index);
                    File file=new File(chat.path);
                    FileInputStream fin=new FileInputStream(file);
                    byte barr[]=new byte[(int)file.length()];
                    fin.read(barr);
                    fin.close();
                    ObjectOutputStream out=new ObjectOutputStream(this.client.getOutputStream());
                    out.writeObject("ChatData");
                    out.writeObject(path);
                    out.writeObject(barr);
                }
                if(req.equals("ChatMesg")){
                    String mesg=in.readObject().toString();
                    int index=Integer.parseInt(in.readObject().toString());
                    Group group=res.CommRes.groups.get(index);
                    
                    ChatDetails cdetails=new ChatDetails();
                    cdetails.path="";
                    cdetails.content=mesg;
                    cdetails.type="text";
                    cdetails.name=this.name;
                    cdetails.sent_on=res.CommRes.getDateTime(Calendar.getInstance());
                    group.chat.add(cdetails);
                    for(int i=0;i<group.active.size();i++)
                    {
                     ClientDetails xdetails=group.active.get(i);
                     ObjectOutputStream tmp=new ObjectOutputStream(xdetails.client.getOutputStream());
                     
                     tmp.writeObject("ChatMesg");
                     tmp.writeObject(index);
                     tmp.writeObject(cdetails);
                     
                    }
                }
                
                if(req.equals("ChatMesgAttach")){
                    byte barr[]=(byte[])in.readObject();
                    String fname=in.readObject().toString();
                    int index=Integer.parseInt(in.readObject().toString());
                    
                    
                    Group group=res.CommRes.groups.get(index);
                    String path="GroupInfo\\" + group.title + "\\" + fname;
                    File file=new File(path);
                    FileOutputStream fout=new FileOutputStream(file);
                    fout.write(barr);
                    fout.close();
                    
                    ChatDetails cdetails=new ChatDetails();
                    cdetails.path=path;
                    cdetails.content="MEDIA[SELECT TO FETCH]";
                    cdetails.type="media";
                    cdetails.name=this.name;
                    cdetails.sent_on=res.CommRes.getDateTime(Calendar.getInstance());
                    group.chat.add(cdetails);
                    for(int i=0;i<group.active.size();i++)
                    {
                     ClientDetails xdetails=group.active.get(i);
                     ObjectOutputStream tmp=new ObjectOutputStream(xdetails.client.getOutputStream());
                     
                     tmp.writeObject("ChatMesg");
                     tmp.writeObject(index);
                     tmp.writeObject(cdetails);
                     
                    }
                }
                
                 if(req.equals("NewAdmin")){
                   int grpindex=Integer.parseInt(in.readObject().toString()); 
                   int userindex=Integer.parseInt(in.readObject().toString());
                   Group group=res.CommRes.groups.get(grpindex);
                   ClientDetails details=group.active.get(userindex);
                   group.admin=details;
                  //group.active.remove(userindex);
                   for(int i=0;i<res.CommRes.onlineUsers.size();i++)
                   {
                    ClientDetails xdetails=res.CommRes.onlineUsers.get(i);
                    ObjectOutputStream tmp=new ObjectOutputStream(xdetails.client.getOutputStream());
                    tmp.writeObject("NewAdmin");
                    tmp.writeObject(grpindex);
                    tmp.writeObject(group.admin);
                   }
               
                } 
                if(req.equals("Logout")){
                        for(int i=0;i<res.CommRes.groups.size();i++){
                            Group grp=res.CommRes.groups.get(i);
                            if(this.id.equals(grp.admin.id)){
                                res.CommRes.groups.remove(i);
                                i--;
                            }
                            else{
                              for( int j=0;j<grp.active.size();j++){
                                if(grp.active.get(j).id.equals(this.id)){      
                                    grp.active.remove(j);
                                    break;
                                }
                              }
                              for( int j=0;j<grp.requests.size();j++){
                                if(grp.active.get(j).id.equals(this.id)){ 
                                    grp.requests.remove(j);
                                    break;
                                }
                              }   
                            }  
                        }
                   for(int i=0;i<res.CommRes.onlineUsers.size();i++){
                    ClientDetails xdetails=res.CommRes.onlineUsers.get(i);
                    if(xdetails.id.equals(this.id)){
                        res.CommRes.onlineUsers.remove(i);
                        break;
                    }
                   }
                   for(int i=0;i<res.CommRes.onlineUsers.size();i++)
                   {
                    ClientDetails xdetails=res.CommRes.onlineUsers.get(i);
                    ObjectOutputStream tmp=new ObjectOutputStream(xdetails.client.getOutputStream());
                    tmp.writeObject("Logout");
                    tmp.writeObject(res.CommRes.groups); 
                   }
                   break;
                }
            }
        }catch(Exception ex){
            
        }
    }

    
   
}
