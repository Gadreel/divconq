package divconq.http.multipart;

/**
 * states follow NOTSTARTED PREAMBLE ( (HEADERDELIMITER DISPOSITION (FIELD |
 * FILEUPLOAD))* (HEADERDELIMITER DISPOSITION MIXEDPREAMBLE (MIXEDDELIMITER
 * MIXEDDISPOSITION MIXEDFILEUPLOAD)+ MIXEDCLOSEDELIMITER)* CLOSEDELIMITER)+
 * EPILOGUE
 *
 * First getStatus is: NOSTARTED
 *
 * Content-type: multipart/form-data, boundary=AaB03x => PREAMBLE in Header
 *
 * --AaB03x => HEADERDELIMITER content-disposition: form-data; name="field1"
 * => DISPOSITION
 *
 * Joe Blow => FIELD --AaB03x => HEADERDELIMITER content-disposition:
 * form-data; name="pics" => DISPOSITION Content-type: multipart/mixed,
 * boundary=BbC04y
 *
 * --BbC04y => MIXEDDELIMITER Content-disposition: attachment;
 * filename="file1.txt" => MIXEDDISPOSITION Content-Type: text/plain
 *
 * ... contents of file1.txt ... => MIXEDFILEUPLOAD --BbC04y =>
 * MIXEDDELIMITER Content-disposition: file; filename="file2.gif" =>
 * MIXEDDISPOSITION Content-type: image/gif Content-Transfer-Encoding:
 * binary
 *
 * ...contents of file2.gif... => MIXEDFILEUPLOAD --BbC04y-- =>
 * MIXEDCLOSEDELIMITER --AaB03x-- => CLOSEDELIMITER
 *
 * Once CLOSEDELIMITER is found, last getStatus is EPILOGUE
 */
public enum MultiPartStatus {
    NOTSTARTED, PREAMBLE, HEADERDELIMITER, DISPOSITION, FIELD, FILEUPLOAD, MIXEDPREAMBLE, MIXEDDELIMITER,
    MIXEDDISPOSITION, MIXEDFILEUPLOAD, MIXEDCLOSEDELIMITER, CLOSEDELIMITER, PREEPILOGUE, EPILOGUE
}