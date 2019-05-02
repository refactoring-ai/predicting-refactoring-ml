import subprocess

total_projects = 12196
total_processed = 0

print("---")
for m in range(1, 31):

	machine_name = "st-ghcrawler" + ("%02d" % m) + ".ewi.tudelft.nl"

	out = subprocess.Popen(["ssh", machine_name, "wc -l execution.txt"], stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
	stdout,stderr = out.communicate()
	p_status = out.wait()

	current = float(stdout.split(' ')[0])
	pct = 0
	if(m == 30):
		pct = current / 365.0
	else:
		pct = current / 407.0

	total_processed = total_processed + current

	print ("* %d=%.2f%%" % (m, pct*100))

print("---")
print("Summary %.2f%%" % (total_processed / total_projects * 100))





