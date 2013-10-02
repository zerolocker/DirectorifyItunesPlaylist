package directorifyplaylist;


import java.io.*;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;
//为这个工具提供技术支持的文章:
//http://support.apple.com/kb/HT1451?viewlocale=zh_CN&locale=zh_CN
//http://hi.baidu.com/liuzy84/item/3b7ffd12f28e73a4feded59f
//http://blog.sina.com.cn/s/blog_521c63500100ch1j.html
// java.net.URLDecoder.decode(param1)

//本java文件可以直接运行,方法是:设置好inputXMLPath,outputXMLPath,运行.
public class DirectorifyPlaylist{
    static Document doc;
    static public String inputXMLPath="E:\\Music\\iTunes\\iTunes Music Library.xml";
    static public String outputXMLPath="C:\\Users\\Zero\\Desktop\\test.xml";
    static public boolean deleteAllOldPlaylists=true;
    public static void main(String [] args){
        try{
        //open the file:
            //ask the user to locate iTunes directory: no need. (done by GUI.java)
            //backup old itl:
            File f=new File(inputXMLPath);
            File olditl=new File(f.getParent()+"\\iTunes Library.itl");
            if(olditl.exists())
                copyFile(olditl,new File(f.getParent()+"\\iTunes Library备份"+Math.round(Math.random()*100)+".itl"));
            //delete old itl:
            olditl.delete();
            //open xml:
             InputStream is=new FileInputStream(inputXMLPath);
            //parse the file:
             DocumentBuilderFactory domfac=DocumentBuilderFactory.newInstance();
             DocumentBuilder dombuilder=domfac.newDocumentBuilder();
             doc=dombuilder.parse(is);
             Element root=doc.getDocumentElement();
             Node dict=getFirstChildByName(root,"dict");dict=getFirstChildByName(dict,"dict");
             
             //build an ArrayList consisting all songs:
             ArrayList<Song> a=new ArrayList<Song>();
             for(Node cur = dict.getFirstChild(); cur != null; cur = cur.getNextSibling()) {
                 Song curSong=new Song();
                 if (cur.getNodeType() == Node.ELEMENT_NODE) {
                     if (cur.getNodeName().equals("key")) {
                         curSong.trackID=cur.getFirstChild().getNodeValue();
                         cur=cur.getNextSibling().getNextSibling();//navigate from the "<key>" to the "<dict>" node
                         Node location=getFirstChildByValue(cur,"Location");
                         Node t=location.getNextSibling();
                         String path=location.getNextSibling().getFirstChild().getNodeValue();
                         curSong.location=path;
                         System.out.println(curSong.trackID+" "+java.net.URLDecoder.decode(curSong.location,"UTF-8"));
                         a.add(curSong);
                     }
                 }
             }
             
             
             //build PlayList:
             Node ArrayInRoot=dict.getNextSibling().getNextSibling().getNextSibling().getNextSibling();
                //remove all previous PlayList:
             if(deleteAllOldPlaylists){
                Node tmp;
                while((tmp=ArrayInRoot.getFirstChild())!=null) ArrayInRoot.removeChild(tmp);
             }
             
             int [] flag=new int[a.size()];
             int hashCnt=0,pathLevel=2;
             for(int i=0;i<flag.length;i++){
                 if(flag[i]==0){
                    hashCnt++;
                    ArrayList<Song> curList=new ArrayList<Song>();
                    for(int j=i;j<flag.length && flag[j]==0;j++){
                        String pathi=shortenFilePath(a.get(i).location,pathLevel),
                               pathj=shortenFilePath(a.get(j).location,pathLevel);
                        if(pathi.equals(pathj)){
                            curList.add(a.get(j));
                            flag[j]=hashCnt;
                        }
                    }
                    String curListName=shortenFilePath(a.get(i).location,pathLevel);
                    CREATENEWLIST(ArrayInRoot,curListName,curList);
                 }
             }
             
             //save the parsed file:
             callWriteXmlFile(doc,outputXMLPath,"UTF-8");
        }catch(Exception e)
        {
            e.printStackTrace();
        }
    }
    public static void CREATENEWLIST(Node ArrayInRoot,String name,ArrayList<Song> songs){
             Node curlist=makeNode("dict",null);ArrayInRoot.appendChild(curlist);
             curlist.appendChild(makeNode("key","Name"));
             curlist.appendChild(makeNode("string",name));
             curlist.appendChild(makeNode("key","Playlist Items"));
             Node array=makeNode("array",null);
             curlist.appendChild(array);
             for(int i=0;i<songs.size();i++){
                 addtoarray(array,songs.get(i));
             }
    }
    public static String shortenFilePath(String filePath,int level){
        //"level" means: if level=2, it tranfers "E:/Music/纯音乐/风居住的街道.mp3" to "Music/纯音乐",
        // if level=3, it tranfers "E:/Music/纯音乐/风居住的街道.mp3" to "E:/Music/纯音乐"
        if(level>3) level=3; //limit "level" to 3
        String res="";
        try {
            filePath=java.net.URLDecoder.decode(filePath, "UTF-8");}
        catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        }
        String [] ss=filePath.split("/");
        for(int i=level;i>0;i--){
            res=res+ss[ss.length-1 -i];
            if(i!=1) res=res+"/";
        }
        return res;
    }
    public static void addtoarray(Node Array,Song song){
        Node dict=makeNode("dict",null);
        dict.appendChild(makeNode("key","Track ID"));
        dict.appendChild(makeNode("integer",song.trackID));
        Array.appendChild(dict);
    }
    public static Node makeNode(String name,String value){
        Node res = doc.createElement(name);
        if(value!=null){
            Text t = doc.createTextNode(value);
            res.appendChild(t);
        }
        return res;
    }
    static class Song{
        public String trackID,location;
    }
    public static Node getFirstChildByName(Node parent,String name){
        NodeList l=parent.getChildNodes();
        for(int i=0;i<l.getLength();i++){
            if(l.item(i).getNodeName().equals(name))
                return l.item(i);
        }
        return null;
    }
    public static Node getFirstChildByValue(Node parent,String value){
        NodeList l=parent.getChildNodes();
        for(int i=0;i<l.getLength();i++){
            if(l.item(i).getFirstChild()!=null && l.item(i).getFirstChild().getNodeValue().equals(value))
                return l.item(i);
        }
        return null;
    }
    public static void callWriteXmlFile(Document doc, String path, String encoding) {
        try {
            Source source = new DOMSource(doc);
            Writer w=new OutputStreamWriter(new FileOutputStream(path),encoding);
            Result result = new StreamResult(w);
            Transformer xformer = TransformerFactory.newInstance()
                    .newTransformer();
            xformer.setOutputProperty(OutputKeys.ENCODING, encoding);
            xformer.transform(source, result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
        public static void copyFile(File oldFile,File newFile) {
            try{
                FileInputStream fis =new FileInputStream(oldFile);
                FileOutputStream fos = new FileOutputStream(newFile);
                byte[] buffer = new byte[1024];//定义一个byte数组，输入与输出的缓存定义为1kb
                int len;                      
                long start,end;
                while((len = fis.read(buffer))!=-1)//让read按照buffer的方法循环read
                 fos.write(buffer,0,len);//将数据按照从零到len的顺序写回文件
                fis.close();
                fos.close();
            }catch(Exception e){e.printStackTrace();}
    }

}