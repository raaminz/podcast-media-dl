@Grab('info.picocli:picocli-groovy:4.6.3')
import static picocli.CommandLine.*
import groovy.transform.Field
import groovy.xml.XmlSlurper

@Command(name = 'podcast-media-dl', mixinStandardHelpOptions = true, version = 'podcast media downloader 0.1',
  description = 'Dowloads all the episode files (including covers and media) of a podcast',
  exitCodeListHeading = "Exit Codes:%n")
@picocli.groovy.PicocliScript

@Option(names = ['-url','u'], description = 'Podcast feed url')
@Field String url

@Option(names = ['-dir','d'], description = 'Directory to copy media files')
@Field File dir

def downloadMedia = { String mUrl, File file ->
    Thread.start {
	while( mUrl ) {
        new URL( mUrl ).openConnection().with { conn ->
        conn.instanceFollowRedirects = false
        mUrl = conn.getHeaderField( "Location" )
        if( !mUrl ) {
           file.withOutputStream { out ->
            conn.inputStream.with { inp ->
              out << inp
              inp.close()
            }
          }
        }
      }
    }
  }
}
def extention= { String fileName ->
        fileName.substring(fileName.lastIndexOf("."));
}



assert url, "URL is null, check --help for more information"

def rootNode = new XmlSlurper().parse(url)

println "URL '$url' downloaded"
assert rootNode.name().equalsIgnoreCase('rss') , "Not a valid RSS!"
def channelName = rootNode.channel.title.text()
println "Podcast name is ${channelName}"
channelName = channelName.replaceAll('/','_')


dir = (dir) ?  new File(dir, channelName) : new File(channelName)
if(!dir.exists()) dir.mkdirs()
println "Copying data into dir '${dir.absolutePath}'"

def urlImage = rootNode.channel.image.url.text()
if(!urlImage){
  urlImage = rootNode.channel.image.@href.text()
}
downloadMedia(urlImage , new File(dir , "cover"+extention(urlImage)))

def all =  rootNode.channel.item.size()

rootNode.channel.item.each {
  
  def subdir = new File(dir, String.valueOf(all--));
  println "Downloading episode data '${it.title.text()}' ..."
  subdir.mkdir();
  def coverUrl = it.image.@href.text()

  if(coverUrl){
    downloadMedia(coverUrl , new File(subdir, "cover"+extention(coverUrl)))
  }

  def mediaUrl = it.enclosure.@url.text()
  if(mediaUrl){
    downloadMedia(mediaUrl , new File(subdir , "content"+extention(mediaUrl)))
  }  

}

