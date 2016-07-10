#lang pollen

◊(define-meta title "Instapaper Auto-send to Kindle not working")
◊(define-meta published "2011-05-23")
◊(define-meta topics "Kindle,Instapaper")

This is a current, unresolved problem: ◊link["http://instapaper.com/"]{Instapaper} does not automatically send new articles to my ◊amazon["B002Y27P3M"]{Kindle} every day. This, despite the fact that my Instapaper account is correctly set up to auto-send new articles to my Kindle, and my Kindle account is properly set up to receive them.

◊figure["kindle.jpg"]{◊i{Manage Your Kindle} on Instapaper's web site}

Here’s the scenario:

◊ol{
◊item{There are new unread articles in my Instapaper account that are more than a day old.}
◊item{My Instapaper account is configured to email new articles to my Kindle every day (see above). I have a Kindle 3 wifi-only.}
◊item{I never receive any emails from Instapaper unless I click the ◊code{Send Now} button. Then, the new Instapaper issue is sent and arrives on my Kindle exactly as expected.}
}

The manual sends arrive just fine; ◊strong{ergo:} the Kindle email address is properly configured, and the Kindle account is properly set up to receive the emails.

The status just above the ◊code{Send Now} button commonly reads “Your last compilation was sent 4 days ago” even when there are new unread articles sitting in the account.

I conclude that Instapaper simply is not sending the emails for some reason. Any guesses as to why?

◊updatebox["No. 1"]{Apparently this is a known issue; ◊amazon["B001U5SPME"]{Wired} published ◊link["http://www.wired.com/gadgetlab/2010/09/how-to-do-almost-everything-with-a-kindle-3/"]{an article} which says “Unfortunately, for reasons I’m not smart enough to understand, Instapaper can’t automate delivery to your ◊code{@free.kindle.com} address.” This tells us nothing about what causes the problem, only that it’s a known issue and, for whatever reason, nothing has been done about it.}

◊updatebox["No. 2"]{Marco seems to have been working on the problem. On May 25, @Instapaper ◊link["https://twitter.com/#!/instapaper/status/73584811683487746"]{tweeted}: “Sending tomorrow morning’s Kindle auto-deliveries early, now. I think I finally found the bug that prevented so many from being delivered.” Then ◊link["https://twitter.com/#!/instapaper/status/73622208060198913"]{on May 26}: “@jeremyisweary Have you changed your Instapaper username since setting it up, possibly when I moved to requiring emails-as-usernames?” The problem has not yet been solved for me personally, or for others, as seen in the comments to this blog post. Will keep you posted.}

◊updatebox["No. 3"]{I’ve been receiving my unread articles from Instapaper on my Kindle, and it seems ◊strong{the problem has been fixed.} On October 5th 2011 (the day Steve Jobs died), @Instapaper ◊link["https://twitter.com/#!/instapaper/status/121779764930428928"]{tweeted} that Marco believed he had fixed the problem. The same day, he ◊link["https://twitter.com/#!/instapaper/status/121780105356906496"]{said}, “By a few hours from now, all deliveries should be made. If you’re one of those who haven’t been getting them, let me know whether you do.” I’ve been getting my deliveries ever since, and so have others.}

If you’re still having problems, let us know in the comments!

◊comment[#:author "nickv"
         #:datetime "May 24, 2011"
         #:authorlink "https://www.blogger.com/profile/18399331908951458413"]{I’m having the same problem, but no solution. Please keep us updated if you figure anything out.}

◊comment[#:author "Marcel"
         #:datetime "May 27, 2011"
         #:authorlink "https://www.blogger.com/profile/05840728983080661886"]{Same issue here - did you get any progress?}

◊comment[#:author "Mike"
         #:datetime "May 30, 2011"
         #:authorlink "https://www.blogger.com/profile/03949187896206172372"]{Yep same problem. It never worked, even though I had daily delivery set and was regularly adding new articles - then I randomly got a delivery the other day --- then nothing since…}

◊comment[#:author "Tiago"
         #:datetime "May 30, 2011"
         #:authorlink "https://www.blogger.com/profile/04637355777980911953"]{Same issue here.}

◊comment[#:author "James"
         #:datetime "June 01, 2011"
         #:authorlink "https://www.blogger.com/profile/13169706717050069732"]{Yep same here --- lets all tweet @instaper and ask?}

◊comment[#:author "nickv"
         #:datetime "June 02, 2011"
         #:authorlink "https://www.blogger.com/profile/18399331908951458413"]{I hit the “reset history” button on ◊link["http://www.instapaper.com/user/kindle"]{IP’s kindle settings page}, and it has successfully made my delivery two days in a row.

I’m not sure why I didn’t try that sooner, as it’s literally the only thing on that page that sounds remotely like it might do something, but hopefully it will help the rest of you too.}

◊comment[#:author "James"
         #:datetime "June 09, 2011"
         #:authorlink "https://www.blogger.com/profile/15529710001567905650"]{Put me down as having this problem as well. I thought I was the only one. I sent email but I’m sure Marco is very busy. I’ve tried resetting the history many times and also created another instapaper account. Neither has worked.}

◊comment[#:author "Joel"
         #:datetime "June 14, 2011"
         #:authorlink "https://www.blogger.com/profile/13646393468637062885"]{There’s been some work on this since late May, apparently - see the updates above. Still not workign though.

If everyone who is still having this problem could tweet this post to @Instapaper, perhaps we could make him more aware of it and help him track down the problem.}

◊comment[#:author "n8henrie"
         #:datetime "June 17, 2011"
         #:authorlink "https://www.blogger.com/profile/01824579072337279035"]{I’ve had this problem on-and-off for months. It seemed to work well if I did the ◊code{@kindle.com} and set my Amazon charge maximum to $0.00, but it has never worked right with the ◊code{@free.kindle.com} address.}

◊comment[#:author "Dr Citrus"
         #:datetime "July 02, 2011"
         #:authorlink "https://www.blogger.com/profile/16265718649436015704"]{I’ve had this issue from day one. Has never worked.}

◊comment[#:author "Marcel"
         #:datetime "July 06, 2011"
         #:authorlink "https://www.blogger.com/profile/05840728983080661886"]{It looks like something has changed and my auto delivery has started to work again. I noticed this a couple of days ago, but it may have been back longer as I had my wireless switched off. Happy Days!}

◊comment[#:author "SoundExplorer"
         #:datetime "July 06, 2011"
         #:authorlink "https://www.blogger.com/profile/10577771258342042112"]{I first tried setting this up about two weeks ago, but it has never worked. Only doing the manual “send now” works. I also find the 20 article limit to be too limiting.}

◊comment[#:author "Dreyfusard"
         #:datetime "July 09, 2011"
         #:authorlink "https://www.blogger.com/profile/01507488016177522587"]{Same here. Only manual send works with free Kindle account. Trying now with non-free address…}

◊comment[#:author "tiede"
         #:datetime "July 10, 2011"
         #:authorlink "http://openid.tiede.dk/"]{Same here - not working}

◊comment[#:author "yioann"
         #:datetime "July 27, 2011"
         #:authorlink "https://www.blogger.com/profile/04656031571092114149"]{same here. it does not work. I will try with ◊code{@kindle.com} and I will let you know.}

◊comment[#:author "yioann"
         #:datetime "August 02, 2011"
         #:authorlink "https://www.blogger.com/profile/04656031571092114149"]{The problem persists. I even deleted my account and recreated it. It still does not work}

◊comment[#:author "yioann"
         #:datetime "August 07, 2011"
         #:authorlink "https://www.blogger.com/profile/04656031571092114149"]{It started working out of the blue today, but I can not trust the service anymore.

I will give ◊link["http://klip.me"] a try. They added a functionality to save articles and send them as a periodical to kindle.}

◊comment[#:author "Gary Cheeseman"
         #:datetime "August 07, 2011"
         #:authorlink "https://www.blogger.com/profile/06156654472173541262"]{I’m having the exact same problem as described in the post too.}

◊comment[#:author "Mark Wills"
         #:datetime "August 24, 2011"
         #:authorlink "https://www.blogger.com/profile/07032214119201061215"]{Not working for me either. :-(}

◊comment[#:author "Dreyfusard"
         #:datetime "August 24, 2011"
         #:authorlink "https://www.blogger.com/profile/01507488016177522587"]{I increasingly turn to Readability for precisely this reason. But Readability does not appear to let one “save” an article from a mobile device…}

◊comment[#:author "sygyzy"
         #:datetime "August 30, 2011"
         #:authorlink "https://www.blogger.com/profile/17681416792612410666"]{Same here. not working for me as well. Very frustrating!}

◊comment[#:author "Kevin"
         #:datetime "September 15, 2011"
         #:authorlink "https://www.blogger.com/profile/13901022813088087935"]{September now. The problem continues…}

◊comment[#:author "Dreyfusard"
         #:datetime "October 06, 2011"
         #:authorlink "https://www.blogger.com/profile/01507488016177522587"]{Unexpectedly received a digest last night. Maybe the problem’s been resolved?}

◊comment[#:author "Joel A."
         #:datetime "October 13, 2011"
         #:authorlink "https://www.blogger.com/profile/13646393468637062885"]{It looks like it’s working! I’ll update the post.}

◊comment[#:author "sebo"
         #:datetime "January 15, 2012"
         #:authorlink "https://www.blogger.com/profile/03477768609763934721"]{I still have this problem; using the ◊code{@kindle.com} address with a Kindle 4 (non-keyboard). Anyone else?}

◊comment[#:author "Federico Solazzo"
         #:datetime "May 30, 2012"
         #:authorlink "https://www.blogger.com/profile/09801446663816990155"]{How to tell Instapaper to send to my Kindle unread posts only once? Because always I still have already sent posts! It always sends me 10 posts, including the ones it already sent me..So, if I mark one to “to read”, it send me 1 unread + 9 already sent. And it’s quite annoying :S}

◊comment[#:author "mihkelgysson"
         #:datetime "July 22, 2012"
         #:authorlink "https://www.blogger.com/profile/11877463872894460936"]{I have the same problem :(

Any help?}
