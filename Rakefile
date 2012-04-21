require 'rake/packagetask'
require 'rexml/document'
require 'erb'


name = 'couch-fuse'
version =  '0.4.1'

name_ver = "#{name}-#{version}"
jar = "#{name_ver}-standalone.jar"
tar = "../#{name_ver}.tar.gz"

def server_path 
  (`uname -a`.include?("i686") && "i386") || "amd64"
end

def lein_with_ld(goal)
  ENV['LD_LIBRARY_PATH'] = "/usr/lib:#{ENV['JAVA_HOME']}/jre/lib/#{server_path}/server:#{pwd}/native/linux/x86_64"
  ENV['LEIN_SNAPSHOTS_IN_RELEASE'] = 'true'
  sh "lein #{goal}"
end

desc 'run lein with ld path'
task :lein , [:task] => [:logfile]  do |t,args|
  lein_with_ld args[:task]
end

task :default => [:package]

task :logfile do
  sh 'sudo touch /var/log/couchfuse.log'
  sh 'sudo chmod 666 /var/log/couchfuse.log'
end

desc 'create jar using lein'
file jar => [:logfile] do
  mkdir 'fake' unless File.exists? 'fake'
  lein_with_ld('uberjar') unless File.exists? jar
end 

file 'couchfuse' do
  path = "/usr/lib/jvm/java-6-sun/jre/lib/#{server_path}/server"
  launch = "java -Dlog4j.configuration='file:/usr/share/couchfuse/log4j.properties' -Djava.library.path=/usr/lib:#{path}:/usr/share/couchfuse/native -jar /usr/share/couchfuse/#{jar} '#{name_ver} filesystem' $@"
  script = "" 
  File.open('packaging/couchfuse.bin' , 'r') { |f| script = f.read }
  template = ERB.new(script)
  File.open('couchfuse' , 'w') do |f|  f.puts template.result(binding) end       
end

file 'native/linux/x86_64/javafs' do
 sh 'lein native-deps'
end

file 'native/linux/x86_64/libjavafs.so' do
 sh 'lein native-deps'
end

Rake::PackageTask.new(name, version) do |pack|
  pack.need_tar_gz = true
  pack.package_files.include('native/linux/x86_64/javafs','native/linux/x86_64/libjavafs.so',jar,'couchfuse')
end

task :clean  do
  %w(pkg native couchfuse).each {|f| rm_r f if File.exists? f}
  sh 'sudo rm -r sandbox' if File.exists? 'sandbox'
  sh 'lein clean'
end

desc 'builds the deb package'
task :deb => [:clean, :sandbox] do 
  ['control','rules','dirs','postinst','prerm'].each{|f| cp "../../packaging/debian/#{f}",'debian/' } 
  sh 'sudo dpkg-buildpackage -b -uc -us'
end

desc 'build the deb sandbox folder'
task :sandbox => [:package] do
  mkdir('sandbox') unless File.exists?('sandbox')
  cp "pkg/#{tar}" , 'sandbox'
  cd 'sandbox'
  sh "tar -xvzf #{tar}"
  mv tar , name_ver
  cd name_ver
  sh "echo 'skip confirmation' | dh_make -e narkisr.dev@gmail.com -c apache -f #{tar} -s -p #{name}_#{version}"
  rm "../#{name}_#{version}.orig.tar.gz"
  Dir['debian/*.ex'].each {|fn| rm fn rescue nil}
end
