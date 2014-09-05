DivConq MFT
===========

DivConq MFT is a free, open-source Managed File Transfer project.  It currently includes a secure file transfer server and a secure gateway.  It may also be extended to handle custom use cases.   

Status
------

DivConq MFT is currently in beta.  The following features are functional:

* File transfers to and from our server with external entities via HTTP(S).
* File transfers through the DMZ using a Gateway

Links
-----

* [Web Site] (http://divconq.com/mft)
* Downloads
* Documentation

Services
--------

We would love to help you build your customized file transfer solution whether based on
DivConq or on another platform.  We are leading file transfer experts with
a background in the MFT industry.  Visit [File Transfer Consulting] (http://www.filetransferconsulting.com/)
for details on who we are and our sevice offerings.

Building
--------

No support for building yet, see downloads on web site for distributed files.

Background
----------

DivConq is short for "divide and conquer."  It refers to our philosophy of using distributed and decoupled components to solve problems.  In the case of DivConq MFT we decouple vertically to solve a security problem (by deploying our gateway in your DMZ and our server in your internal network), and decouple horizontally to solve a scalability problem (by deploying multiple nodes in each layer).

DivConq MFT was originally built as a custom project to support high-volume bulk data transfers for a data escrow company located in the US and UK.  That project required us to accept, decrypt, validate and archive the contents of large, well-structured files encrypted with OpenPGP.  The implementation was specific to that corporation's needs, but we are developing DivConq MFT to handle general managed file transfer use cases through configurable options.   

Ultimately, the DivConq MFT roadmap includes the following features:

* File transfers with external entities via HTTP(S), SFTP and FTP(S).
* File transfers through the DMZ using a Gateway
* File tasks including scheduled tasks and triggered tasks
* File/deposit validation framework
* High level scripting for managing file workflow
* Flexible and extensible Web User Interface
* Flexible and extensible server components
* Built-in support for OpenPGP
