#!/usr/bin/env ruby

index = 1

sets = {"attacks" => "MW1A"}
sets = {"creatures" => "MW1C"}

sets.each {|k, v|
  text = File.read("#{k}.edn")
  new_text = ""
  text.each_line {|line|
    if line =~ /:description/
      num = sprintf("%02d", index);
      index += 1;
      new_text += line.sub(Regexp.new('(:description .*")}'), "\\1\n   :image \"#{v}#{num}.jpg\"}")
    else
      new_text += line
    end
  }
  puts new_text
}
