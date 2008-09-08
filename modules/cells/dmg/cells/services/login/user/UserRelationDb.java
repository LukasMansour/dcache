// $Id: UserRelationDb.java,v 1.2 2006-12-15 10:58:14 tigran Exp $
package dmg.cells.services.login.user  ;
import java.util.* ;
import java.io.* ;

public class UserRelationDb {

    private class DEnumeration<T> implements Enumeration<T> {
        public boolean hasMoreElements(){ return false ; }
        public T nextElement(){ return null ;}
    }
    private class ElementItem {
       private Hashtable<String, String> _parents = null ;
       private Hashtable<String, String> _childs  = null ;
       private void addParent(String parent){
          if( _parents == null )_parents = new Hashtable<String, String>() ;
          _parents.put(parent,parent) ;
       }
       private void addChild( String child ){
          if( _childs == null )_childs = new Hashtable<String, String>() ;
          _childs.put(child,child) ;
       }
       private Enumeration<String> parents(){
           return _parents == null ? new DEnumeration<String>() : _parents.keys() ;
       }
       private Enumeration<String> children(){
           return _childs == null ? new DEnumeration<String>() : _childs.keys() ;
       }
    }
    private File      _dbDir    = null ;
    private Hashtable<String, ElementItem> _elements = null ;
    public UserRelationDb( File dbDir )throws DatabaseException {
        if( ( ! dbDir.exists() ) || ( ! dbDir.isDirectory() ) )
          throw new
          DatabaseException( 11 , "Not a directory : "+dbDir ) ;

        _dbDir = dbDir ;

        _loadElements() ;
    }
    public static  Map<String, Boolean> loadAcl( File aclFile ){
       Map<String, Boolean>      acl = new Hashtable<String, Boolean>() ;
       BufferedReader br  = null ;
       try{
          br = new BufferedReader(
                    new FileReader( aclFile ) ) ;
          String line  = null ;
          StringTokenizer st = null ;
          String name  = null ;
          String value = null ;
          while( ( line = br.readLine() ) != null ){
             st = new StringTokenizer(line,"=") ;
             try{
                name  = st.nextToken() ;
                value = st.nextToken() ;
                acl.put( name ,
                         Boolean.valueOf( value.equals("allowed") )
                       ) ;
             }catch(Exception ee){
                continue ;
             }
          }
       }catch( IOException ie ){
       }finally{
          if( br != null ) { try{ br.close() ; }catch(IOException ee){} }
       }
       return acl ;
    }
    public void display(){
       Enumeration<String> all = _elements.keys() ;
       while( all.hasMoreElements() ){
           String name = all.nextElement() ;
           ElementItem item = _elements.get(name) ;
           System.out.println(name);
           Enumeration<String> e = item.parents() ;
           while( e.hasMoreElements() ){
              System.out.println("   p:"+e.nextElement());
           }
           e = item.children() ;
           while( e.hasMoreElements() ){
              System.out.println("   c:"+e.nextElement());
           }
       }
    }
    private void _loadElements() throws DatabaseException{
        String [] elements = _dbDir.list(
                     new FilenameFilter(){
                        public boolean accept( File dir , String name ){
                           return ! name.startsWith(".") ;
                        }
                     } ) ;
        Hashtable<String,ElementItem > hash = new Hashtable<String,ElementItem >() ;
        for( int i = 0 ; i < elements.length ; i++ ){
            File file = new File( _dbDir , elements[i] ) ;
            BufferedReader br = null ;
            ElementItem item  = null , x = null;
            if( ( item = hash.get( elements[i] ) ) == null ){
                hash.put( elements[i] , item = new ElementItem() ) ;
            }
            try{
               br = new BufferedReader(
                         new FileReader( file ) ) ;
               String line = null ;
               while( ( line = br.readLine() ) != null ){
                   String name = line.trim() ;
                   if( name.length() == 0 )continue ;
                   if( name.charAt(0) == '#' )continue ;
                   item.addChild(name) ;
                   if( ( x = hash.get(name) ) == null ){
                       hash.put(name , x = new ElementItem() ) ;
                   }
                   x.addParent( elements[i] ) ;
               }
            }catch( IOException ie ){
            }finally{
               if( br != null ) { try{ br.close() ; }catch(IOException ee){} }
            }
        }
        _elements = hash ;
    }
    public boolean check( String user ,  Map<String, Boolean> acl ){
        Boolean ok = null ;
        if( ( ok = acl.get(user) ) != null )return ok.booleanValue() ;

        Vector<String> v = new Vector<String>() ;
        String p = null ;
        ElementItem item = null ;
        Boolean     x    = null ;
        v.addElement( user ) ;

        for( int i = 0 ; i < v.size() ; i++ ){
            p = v.elementAt(i) ;
            if( ( x = acl.get(p) ) != null ){
               if( x.booleanValue() )return true ;
               continue ;
            }
            if( (item = _elements.get(p)) != null ){
               Enumeration<String> e = item.parents() ;
               while( e.hasMoreElements() )
                  v.addElement(e.nextElement()) ;
            }
        }
        return false ;
    }
    public static void main( String [] args ) throws Exception {
       if( args.length < 1 ){
          System.err.println( "Usage : ... <dbDirectory> [<acl> <user>]");
          System.exit(4);
       }
       File   dbDir   = new File(args[0]);

       UserRelationDb db = new UserRelationDb( dbDir ) ;
       if( args.length < 3 ){
          db.display();
       }else{
          File   aclFile = new File( args[1] ) ;
          String user    = args[2] ;
          Map<String, Boolean> acl = UserRelationDb.loadAcl( aclFile ) ;
          boolean rc = db.check( user , acl ) ;
          System.out.println( "user="+user+";acl="+aclFile+";allowed="+rc) ;
       }
    }
}

