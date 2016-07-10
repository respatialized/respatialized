#lang pollen

◊(define-meta title "‘No Audio Output Device Is Installed’ — Vista Error")
◊(define-meta published "2009-03-05")
◊(define-meta topics "Windows problems")

So, just tonight, my laptop suddenly decided it had no sound. The speaker icon in the tray had a red ◊color["red"]{X} over it and hovering over it produced a little error message: “No Audio Output Device Is Installed”. This is of course surprising since I’d had audio before, and no service packs or new drivers had been installed anytime recently (they were all up to date already). A look inside Device Manager confirmed that I did in fact have drivers installed and they were working properly. A little searching confirmed that this issue was not uncommon. I finally tried the first likely fix I found, buried in ◊link["http://forums.cnet.com/5208-12546_102-0.html?forumID=133&threadID=246671&messageID=2481071"]{this CNET post}, and it worked. Essentially, the problem is the result of some kind of conflict between the modem and sound card drivers. I uninstalled both drivers in the Device Manager (do not select the option to “remove from system” or anything like that if given the choice) and restarted the machine. After the restart, Windows found the both devices and automatically reinstalled the drivers for them, and everything was working perfectly. Yay!
