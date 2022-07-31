set xdata time                              # x-axis values are time (date) values
set timefmt "%Y-%m-%d"                      # date value format
set datafile separator ";"				    # CSV column separator is semicolon
set key autotitle columnhead			    # first data line contains column titles
set format x "%y-%m-%d"                     # display date format

set xrange ["2022-07-11":"2022-10-31"]      # x-axis value-range
set yrange [0:600]                          # y-axis value-range

set key inside                              # graph legend style
set xtics rotate by -45                     # rotate dates on x-axis 45deg for cleaner display
set title 'hsadmin-ng Projektfortschritt'   # graph title

set terminal png                            # output format
set term png size 920, 640				    # output canvas size
set output 'TODO-progress.png'              # output file name

plot '.todo-progress.csv' 	using 1:2 with linespoints linetype rgb "black" linewidth 2, \
	 '' 					using 1:3 with linespoints linetype rgb "red" 	linewidth 2, \
     '' 					using 1:4 with linespoints linetype rgb "green" linewidth 2, \
     '' 					using 1:5 with linespoints linetype rgb "blue" 	linewidth 2


