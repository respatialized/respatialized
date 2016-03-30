◊(local-require racket/file racket/system racket/list)
◊(local-require "../util-date.rkt")
◊(define (print-if thing fmt)
   (if thing (format fmt thing) ""))
◊(define latex-source ◊string-append{
    \documentclass[12pt]{article}

    \usepackage{amssymb,amsmath}

    \usepackage[T1]{fontenc}
    %\usepackage[utf8]{inputenc}

    \usepackage{eurosym}
    \usepackage{fancyvrb}
    \usepackage{longtable,booktabs}
    \usepackage{attrib}
    \usepackage{graphicx}
    \usepackage{mathspec}
    \usepackage{xltxtra,xunicode}

    \defaultfontfeatures{Scale=MatchLowercase}

    \usepackage{microtype}
    \usepackage{fontspec}

    %% Typography defaults
    \newfontfamily\linenumberfont[Mapping=tex-text]{Fira Mono OT}

    \setsansfont[Mapping=tex-text]{Fira Sans OT}
    \setmainfont[Mapping=tex-text]{Charter}
    \setmonofont{Triplicate T4c}

    \usepackage{color}
    \definecolor{mygray}{rgb}{0.7,0.7,0.7}
    \definecolor{light-gray}{gray}{0.95}

    \usepackage{textcomp}
    \usepackage{upquote}
    \usepackage{listings}
    \lstset{
        basicstyle=\small\ttfamily,
        columns=flexible,
        breaklines=true,
        numbers=left,
        upquote=true,
        backgroundcolor=\color{light-gray},
        numbersep=5pt,
        xleftmargin=.25in,
        xrightmargin=.25in,
        framexleftmargin=.25in,
        numberstyle=\small\color{mygray}\linenumberfont
    }

    \makeatletter
    \def\maxwidth{\ifdim\Gin@nat@width>\linewidth\linewidth\else\Gin@nat@width\fi}
    \def\maxheight{\ifdim\Gin@nat@height>\textheight\textheight\else\Gin@nat@height\fi}
    \makeatother

    % Scale images if necessary, so that they will not overflow the page
    % margins by default, and it is still possible to overwrite the defaults
    % using explicit options in \includegraphics[width, height, ...]{}
    \setkeys{Gin}{width=\maxwidth,height=\maxheight,keepaspectratio}

      \usepackage[setpagesize=false, % page size defined by xetex
                  unicode=false, % unicode breaks when used with xetex
                  xetex]{hyperref}

    \hypersetup{breaklinks=true,
                bookmarks=true,
                pdfauthor={◊(print-if (select-from-metas 'author metas) "~a")},
                pdftitle={◊(print-if (select-from-metas 'title metas) "~a")},
                colorlinks=true,
                citecolor=blue,
                urlcolor=blue,
                linkcolor=magenta,
                pdfborder={0 0 0}}
    \urlstyle{same}  % don't use monospace font for urls

    % Make links footnotes instead of hotlinks:
    \renewcommand{\href}[2]{#2\footnote{\url{#1}}}

    % Make margin notes (from Tufte-LaTeX) into regular footnotes
    \newcommand{\marginnote}[1]{\footnote{#1}}
    \newcommand{\smallcaps}[1]{\textsc{#1}}

    \setlength{\parindent}{0pt}
    \setlength{\parskip}{6pt plus 2pt minus 1pt}
    \setlength{\emergencystretch}{3em}  % prevent overfull lines

    \setcounter{secnumdepth}{0}

    \VerbatimFootnotes % allows verbatim text in footnotes

    \title{◊(print-if (select-from-metas 'title metas) "~a")}
    \author{◊(print-if (select-from-metas 'author metas) "~a")}
    \date{◊(unless (not (select-from-metas 'published metas)) (pubdate->english (select-from-metas 'published metas)))}

    %% Reduced margins
    %\usepackage[margin=1.2in]{geometry}

    %% Paragraph and line spacing
    %\linespread{1.05} % a bit more vertical space
    %\setlength{\parskip}{\baselineskip} % space between paragraphs spacing is one baseline unit

    %% Sections headings spacing: one baseline unit before, none after
    \usepackage{titlesec}
    \titlespacing{\section}{0pt}{\baselineskip}{0pt}
    \titlespacing{\subsection}{0pt}{\baselineskip}{0pt}
    \titlespacing{\subsubsection}{0pt}{\baselineskip}{0pt}

    % Customize footnotes so that, within the footnote, the footnote number is
    % the same size as the footnote text (per Bringhurst).
    %
    \usepackage[splitrule,multiple,hang]{footmisc}
    \makeatletter
    \renewcommand\@makefntext[1]{\parindent 1em%
        \noindent
        \hb@xt@0em{\hss\normalfont\@thefnmark.} #1}
    \def\splitfootnoterule{\kern-3\p@ \hrule width 1in \kern2.6\p@}
    \makeatother
    \renewcommand\footnotesize{\fontsize{10}{12} \selectfont}
    \renewcommand{\thefootnote}{\arabic{footnote}}

    %% Main doc
    \begin{document}

    \maketitle

    ◊(apply string-append (cdr doc))

    \end{document}
})

◊(define working-directory
    (build-path (current-directory) "pollen-latex-work"))
◊(unless (directory-exists? working-directory)
    (make-directory working-directory))
◊(define temp-ltx-path (build-path working-directory "temp.ltx"))
◊(display-to-file latex-source temp-ltx-path #:exists 'replace)
◊(define command (format "xelatex -output-directory='~a' '~a'" working-directory temp-ltx-path))
◊(if (system command)
    (file->bytes (build-path working-directory "temp.pdf"))
    (error "xelatex: rendering error"))