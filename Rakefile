require 'rake/packagetask'
require 'rexml/document'
require 'erb'


name = 'couch-fuse'
version =  '0.4.1'

NAME_VER = "#{name}-#{version}"
JAR = "#{NAME_VER}-standalone.jar"
DEB = "couchfuse_#{version}_amd64.deb"

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

desc 'create JAR using lein'
file JAR => [:logfile] do
  mkdir 'fake' unless File.exists? 'fake'
  lein_with_ld('uberjar') unless File.exists? JAR
end 

file 'couchfuse' do
  java_path = "/usr/lib/jvm/java-6-sun/jre/lib/#{server_path}/server"
  parent = '/usr/share/couchfuse'
  native_path = "#{parent}/native/linux/x86_64"
  launch = "java -Dlog4j.configuration='file:#{parent}/log4j.properties' -Djava.library.path=/usr/lib:#{java_path}:#{native_path} -jar #{parent}/#{JAR} '#{NAME_VER} filesystem' $@"
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

file 'log4j.properties' do
  cp 'resources/log4j.properties', 'log4j.properties'
end

Rake::PackageTask.new(name, version) do |pack|
  pack.need_tar_gz = true
  pack.package_files.include('native/linux/x86_64/javafs','native/linux/x86_64/libjavafs.so',JAR,'couchfuse','log4j.properties')
end

task :clean  do
  ([DEB] + %w(pkg native couchfuse log4j.properties)).each {|f| rm_r f if File.exists? f}
  sh 'sudo rm -r sandbox' if File.exists? 'sandbox'
  sh 'lein clean'
end

desc 'builds the deb package'
task :deb => [:clean, :package] do 
  sh "fpm -s dir -t deb -n couchfuse -v #{version} --prefix /usr/share/couchfuse -d 'sun-java6-jre (>= 6-15-1)' -d 'fuse-utils' --post-install packaging/post_install.sh --post-uninstall  packaging/post_uninstall.sh -C pkg/#{NAME_VER} . "
end

